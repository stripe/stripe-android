package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Stripe
import com.stripe.android.checkout.CheckoutElementsController.Address
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.MeasuredOperation
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.networking.ConfirmationParamsUtils
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Provider

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val elementsSessionClientParams: ElementsSessionClientParams,
    private val stripeNetworkClient: StripeNetworkClient,
    private val fraudDetectionDataRepository: FraudDetectionDataRepository,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val apiRequestOptionsProvider: Provider<ApiRequest.Options>,
) {

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = Stripe.appInfo,
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
    )
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private suspend fun executePost(
        clientSecret: CheckoutSessionClientSecret,
        url: String,
        params: Map<String, *>,
    ): Result<CheckoutSessionResponse> {
        val options = apiRequestOptionsProvider.get()
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = url,
                options = options,
                params = elementsSessionClientParams.toCheckoutSessionMap() + params +
                    ("client_secret" to clientSecret.value),
            ),
            responseJsonParser = CheckoutSessionResponseJsonParser(clientSecret),
        )
    }

    suspend fun init(
        clientSecret: CheckoutSessionClientSecret,
        adaptivePricingAllowed: Boolean,
        analyticsParams: Map<String, Any?>,
    ): Result<CheckoutSessionResponse> = measure(
        operation = Operation.Init,
        checkoutSessionResponse = null,
        params = analyticsParams + ("checkout_session_id" to clientSecret.sessionId),
    ) {
        executePost(
            clientSecret = clientSecret,
            url = initUrl(clientSecret.sessionId),
            params = mapOf(
                "adaptive_pricing[allowed]" to adaptivePricingAllowed,
                "timezone" to TimeZone.getDefault().id,
            ),
        )
    }

    suspend fun confirm(
        id: String,
        clientSecret: CheckoutSessionClientSecret,
        params: ConfirmCheckoutSessionParams,
    ): Result<CheckoutSessionResponse> {
        fraudDetectionDataRepository.refresh()

        val requestParams = ConfirmationParamsUtils.prepareConfirmationParams(
            params.toParamMap(),
            params.paymentMethodData,
            sourceParams = null,
            fraudDetectionData = fraudDetectionDataRepository.getCached(),
            productUsageTokens = setOf("checkout_elements"),
        )
        return executePost(
            clientSecret = clientSecret,
            url = confirmUrl(id),
            params = requestParams,
        ).mapCatching { response ->
            check(response.status != CheckoutSessionResponse.Status.EXPIRED) {
                "Checkout Session unexpectedly expired after confirmation."
            }
            if (response.paymentResponse?.status == "requires_payment_method") {
                throw InvalidRequestException(
                    stripeError = response.paymentResponse.error,
                    message = response.paymentResponse.error?.message ?: "Checkout requires a payment method.",
                )
            }
            val paymentReference = response.paymentResponse?.paymentIntent
            val setupReference = response.paymentResponse?.setupIntent
            val intent = when {
                paymentReference != null -> stripeRepository.retrievePaymentIntent(
                    clientSecret = paymentReference.clientSecret,
                    options = apiRequestOptionsProvider.get(),
                    expandFields = listOf("payment_method"),
                ).getOrThrow()
                setupReference != null -> stripeRepository.retrieveSetupIntent(
                    clientSecret = setupReference.clientSecret,
                    options = apiRequestOptionsProvider.get(),
                    expandFields = listOf("payment_method"),
                ).getOrThrow()
                else -> error(
                    if (response.paymentResponse?.status == "requires_action") {
                        "CheckoutClient requires an action without a supported Intent."
                    } else {
                        "No PaymentIntent or SetupIntent in checkout session confirm response"
                    }
                )
            }
            response.withIntent(intent)
        }
    }

    suspend fun poll(
        sessionId: String,
        clientSecret: CheckoutSessionClientSecret,
    ): Result<CheckoutClientPollResponse> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createGet(
                url = pollUrl(sessionId),
                options = apiRequestOptionsProvider.get(),
                params = mapOf("client_secret" to clientSecret.value),
            ),
            responseJsonParser = CheckoutClientPollResponseJsonParser,
        )
    }

    suspend fun detachPaymentMethod(
        sessionId: String,
        clientSecret: CheckoutSessionClientSecret,
        paymentMethodId: String,
    ): Result<CheckoutSessionResponse> = executePost(
        clientSecret = clientSecret,
        url = updateUrl(sessionId),
        params = mapOf(
            "payment_method_to_detach" to paymentMethodId,
        ),
    )

    suspend fun updatePaymentMethod(
        sessionId: String,
        clientSecret: CheckoutSessionClientSecret,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<CheckoutSessionResponse> {
        val card = params as? PaymentMethodUpdateParams.Card
        val updateParams = CheckoutSessionUpdatePaymentMethodParams(
            paymentMethodId = paymentMethodId,
            expiryMonth = card?.expiryMonth,
            expiryYear = card?.expiryYear,
            billingDetails = params.billingDetails,
        )

        return if (updateParams.hasSupportedUpdates) {
            executePost(
                clientSecret = clientSecret,
                url = updateUrl(sessionId),
                params = updateParams.toParamMap(),
            )
        } else {
            Result.failure(IllegalArgumentException(UNSUPPORTED_UPDATE_ERROR))
        }
    }

    suspend fun applyPromotionCode(
        checkoutSessionResponse: CheckoutSessionResponse,
        promotionCode: String,
    ): Result<CheckoutSessionResponse> = measure(
        operation = Operation.ApplyPromotionCode,
        checkoutSessionResponse = checkoutSessionResponse,
        params = emptyMap(),
    ) {
        executePost(
            clientSecret = checkoutSessionResponse.clientSecret,
            url = updateUrl(checkoutSessionResponse.id),
            params = mapOf(
                "promotion_code" to promotionCode,
            ),
        )
    }

    suspend fun removePromotionCode(
        checkoutSessionResponse: CheckoutSessionResponse,
    ): Result<CheckoutSessionResponse> = measure(
        operation = Operation.RemovePromotionCode,
        checkoutSessionResponse = checkoutSessionResponse,
        params = emptyMap(),
    ) {
        executePost(
            clientSecret = checkoutSessionResponse.clientSecret,
            url = updateUrl(checkoutSessionResponse.id),
            params = mapOf(
                "promotion_code" to "",
            ),
        )
    }

    suspend fun updateTaxRegion(
        sessionId: String,
        clientSecret: CheckoutSessionClientSecret,
        address: Address.State?,
    ): Result<CheckoutSessionResponse> = executePost(
        clientSecret = clientSecret,
        url = updateUrl(sessionId),
        params = buildMap {
            if (address == null) {
                put("tax_region", "")
                return@buildMap
            }
            putIfNotEmpty("tax_region[country]", address.country)
            putIfNotEmpty("tax_region[line1]", address.line1)
            putIfNotEmpty("tax_region[line2]", address.line2)
            putIfNotEmpty("tax_region[city]", address.city)
            putIfNotEmpty("tax_region[state]", address.state)
            putIfNotEmpty("tax_region[postal_code]", address.postalCode)
        },
    )

    suspend fun updateCurrency(
        sessionId: String,
        clientSecret: CheckoutSessionClientSecret,
        currencyCode: String,
    ): Result<CheckoutSessionResponse> = executePost(
        clientSecret = clientSecret,
        url = updateUrl(sessionId),
        params = mapOf(
            "adaptive_pricing[currency]" to currencyCode,
        ),
    ).onSuccess {
        fireEvent(PaymentSheetEvent.AdaptivePricingCurrencyToggled())
    }.onFailure {
        fireEvent(PaymentSheetEvent.AdaptivePricingCurrencyToggledFailed(error = it.safeAnalyticsMessage))
    }

    private suspend fun measure(
        operation: Operation,
        checkoutSessionResponse: CheckoutSessionResponse?,
        params: Map<String, Any?>,
        block: suspend () -> Result<CheckoutSessionResponse>,
    ): Result<CheckoutSessionResponse> {
        val measuredOperation = MeasuredOperation(
            operation = operation,
            params = params,
            report = ::fireEvent,
        )
        measuredOperation.started(checkoutSessionResponse)
        return block()
            .onSuccess { measuredOperation.succeeded(it) }
            .onFailure { measuredOperation.failed(checkoutSessionResponse, it) }
    }

    private fun fireEvent(event: PaymentSheetEvent) {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params,
            )
        )
    }

    private fun fireEvent(event: CheckoutAnalyticsEvent) {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params,
            )
        )
    }

    private companion object {
        private const val UNSUPPORTED_UPDATE_ERROR =
            "Checkout session update requires at least card expiry or billing details."

        private fun initUrl(sessionId: String): String =
            "${ApiRequest.API_HOST}/v1/checkout_client/$sessionId/init"

        private fun confirmUrl(checkoutSessionId: String): String =
            "${ApiRequest.API_HOST}/v1/checkout_client/$checkoutSessionId/confirm"

        private fun updateUrl(sessionId: String): String =
            "${ApiRequest.API_HOST}/v1/checkout_client/$sessionId/update"

        private fun pollUrl(sessionId: String): String =
            "${ApiRequest.API_HOST}/v1/checkout_client/$sessionId/poll"
    }
}

private fun MutableMap<String, Any>.putIfNotEmpty(key: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        put(key, value)
    }
}

package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Stripe
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.networking.ConfirmationParamsUtils
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import java.io.OutputStream
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
        clientSecret: String,
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
                    ("client_secret" to clientSecret),
            ),
            responseJsonParser = CheckoutSessionResponseJsonParser(clientSecret),
        )
    }

    suspend fun init(
        sessionId: String,
        clientSecret: String,
        adaptivePricingAllowed: Boolean,
    ): Result<CheckoutSessionResponse> {
        return executePost(
            clientSecret = clientSecret,
            url = initUrl(sessionId),
            params = mapOf(
                "adaptive_pricing[allowed]" to adaptivePricingAllowed,
                "timezone" to TimeZone.getDefault().id,
            ),
        )
    }

    suspend fun confirm(
        id: String,
        clientSecret: String,
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
        clientSecret: String,
    ): Result<CheckoutClientPollResponse> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = NonRetryingRequest(
                apiRequestFactory.createGet(
                    url = pollUrl(sessionId),
                    options = apiRequestOptionsProvider.get(),
                    params = mapOf("client_secret" to clientSecret),
                )
            ),
            responseJsonParser = CheckoutClientPollResponseJsonParser,
        )
    }

    /**
     * Retrieves the full Checkout Session without retries, e.g. to read payment errors after polling.
     */
    suspend fun retrieve(
        sessionId: String,
        clientSecret: String,
    ): Result<CheckoutSessionResponse> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = NonRetryingRequest(
                apiRequestFactory.createGet(
                    url = retrieveUrl(sessionId),
                    options = apiRequestOptionsProvider.get(),
                    params = elementsSessionClientParams.toCheckoutSessionMap() + mapOf(
                        "timezone" to TimeZone.getDefault().id,
                        "client_secret" to clientSecret,
                    ),
                )
            ),
            responseJsonParser = CheckoutSessionResponseJsonParser(clientSecret),
        )
    }

    suspend fun detachPaymentMethod(
        sessionId: String,
        clientSecret: String,
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
        clientSecret: String,
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
        sessionId: String,
        clientSecret: String,
        promotionCode: String,
    ): Result<CheckoutSessionResponse> = executePost(
        clientSecret = clientSecret,
        url = updateUrl(sessionId),
        params = mapOf(
            "promotion_code" to promotionCode,
        ),
    )

    suspend fun updateTaxRegion(
        sessionId: String,
        clientSecret: String,
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
        clientSecret: String,
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

    private fun fireEvent(event: PaymentSheetEvent) {
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

        private fun retrieveUrl(sessionId: String): String =
            "${ApiRequest.API_HOST}/v1/checkout_client/$sessionId/retrieve"

        private fun pollUrl(sessionId: String): String =
            "${ApiRequest.API_HOST}/v1/checkout_client/$sessionId/poll"
    }
}

private fun MutableMap<String, Any>.putIfNotEmpty(key: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        put(key, value)
    }
}

/**
 * Callers own retry and backoff decisions, so the network client must not also retry 429s.
 */
private class NonRetryingRequest(
    private val request: ApiRequest,
) : StripeRequest() {
    override val method: Method = request.method
    override val mimeType: MimeType = request.mimeType
    override val retryResponseCodes: Iterable<Int> = emptyList()
    override val url: String = request.url
    override val headers: Map<String, String> = request.headers

    override var postHeaders: Map<String, String>? = request.postHeaders

    override fun writePostBody(outputStream: OutputStream) = request.writePostBody(outputStream)

    override fun toString(): String = request.toString()
}

package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Stripe
import com.stripe.android.checkout.Address
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionRepository @Inject constructor(
    private val clientParams: ElementsSessionClientParams,
    private val stripeNetworkClient: StripeNetworkClient,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
) {

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = Stripe.appInfo,
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
    )
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private suspend fun executePost(
        url: String,
        params: Map<String, *>,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> {
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = url,
                options = requestOptions,
                params = params,
            ),
            responseJsonParser = CheckoutSessionResponseJsonParser,
        )
    }

    suspend fun init(
        sessionId: String,
        adaptivePricingAllowed: Boolean,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> {
        return executePost(
            url = initUrl(sessionId),
            params = mapOf(
                "browser_locale" to clientParams.locale,
                "browser_timezone" to TimeZone.getDefault().id,
                "eid" to UUID.randomUUID().toString(),
                "redirect_type" to "embedded",
                "elements_session_client" to clientParams.toCheckoutSessionMap(),
                "adaptive_pricing[allowed]" to adaptivePricingAllowed.toString(),
            ),
            requestOptions = requestOptions,
        )
    }

    suspend fun confirm(
        id: String,
        params: ConfirmCheckoutSessionParams,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = confirmUrl(id),
        params = params.toParamMap().plus(Pair("elements_session_client[is_aggregation_expected]", "true")),
        requestOptions = requestOptions,
    )

    suspend fun detachPaymentMethod(
        sessionId: String,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "payment_method_to_detach" to paymentMethodId,
        ),
        requestOptions = requestOptions,
    )

    suspend fun updatePaymentMethod(
        sessionId: String,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
        requestOptions: ApiRequest.Options,
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
                url = updateUrl(sessionId),
                params = updateParams.toParamMap(),
                requestOptions = requestOptions,
            )
        } else {
            Result.failure(IllegalArgumentException(UNSUPPORTED_UPDATE_ERROR))
        }
    }

    suspend fun applyPromotionCode(
        sessionId: String,
        promotionCode: String,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "promotion_code" to promotionCode,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
        requestOptions = requestOptions,
    )

    suspend fun updateLineItemQuantity(
        sessionId: String,
        lineItemId: String,
        quantity: Int,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "updated_line_item_quantity[line_item_id]" to lineItemId,
            "updated_line_item_quantity[quantity]" to quantity.toString(),
            "updated_line_item_quantity[fail_update_on_discount_error]" to "true",
        ),
        requestOptions = requestOptions,
    )

    suspend fun selectShippingRate(
        sessionId: String,
        shippingRateId: String,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "shipping_rate" to shippingRateId,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
        requestOptions = requestOptions,
    )

    suspend fun updateTaxRegion(
        sessionId: String,
        address: Address.State,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = buildMap {
            putIfNotEmpty("tax_region[country]", address.country)
            putIfNotEmpty("tax_region[line1]", address.line1)
            putIfNotEmpty("tax_region[line2]", address.line2)
            putIfNotEmpty("tax_region[city]", address.city)
            putIfNotEmpty("tax_region[state]", address.state)
            putIfNotEmpty("tax_region[postal_code]", address.postalCode)
            put("elements_session_client[is_aggregation_expected]", "true")
        },
        requestOptions = requestOptions,
    )

    suspend fun updateTaxId(
        sessionId: String,
        type: String,
        value: String,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "tax_id_collection[tax_id][type]" to type,
            "tax_id_collection[tax_id][value]" to value,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
        requestOptions = requestOptions,
    )

    suspend fun updateCurrency(
        sessionId: String,
        currencyCode: String,
        requestOptions: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "updated_currency" to currencyCode,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
        requestOptions = requestOptions,
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
            "${ApiRequest.API_HOST}/v1/payment_pages/$sessionId/init"

        private fun confirmUrl(checkoutSessionId: String): String =
            "${ApiRequest.API_HOST}/v1/payment_pages/$checkoutSessionId/confirm"

        private fun updateUrl(sessionId: String): String =
            "${ApiRequest.API_HOST}/v1/payment_pages/$sessionId"
    }
}

private fun MutableMap<String, Any>.putIfNotEmpty(key: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        put(key, value)
    }
}

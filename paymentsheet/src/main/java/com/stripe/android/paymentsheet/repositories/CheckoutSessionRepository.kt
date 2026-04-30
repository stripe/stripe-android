package com.stripe.android.paymentsheet.repositories

import android.content.Context
import com.stripe.android.Stripe
import com.stripe.android.checkout.Address
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.paymentelement.CheckoutSessionPreview
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionRepository @Inject constructor(
    private val context: Context,
    private val stripeNetworkClient: StripeNetworkClient,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
) {
    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = Stripe.appInfo,
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
    )
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private fun createOptions(): ApiRequest.Options = ApiRequest.Options(
        apiKey = publishableKeyProvider(),
        stripeAccount = stripeAccountIdProvider(),
    )

    private suspend fun executePost(
        url: String,
        params: Map<String, *>,
    ): Result<CheckoutSessionResponse> {
        val options = createOptions()
        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                url = url,
                options = options,
                params = params,
            ),
            responseJsonParser = CheckoutSessionResponseJsonParser(
                isLiveMode = options.apiKeyIsLiveMode,
            ),
        )
    }

    suspend fun init(
        sessionId: String,
        adaptivePricingAllowed: Boolean,
    ): Result<CheckoutSessionResponse> = executePost(
        url = initUrl(sessionId),
        params = mapOf(
            "browser_locale" to Locale.getDefault().toLanguageTag(),
            "browser_timezone" to TimeZone.getDefault().id,
            "eid" to UUID.randomUUID().toString(),
            "redirect_type" to "embedded",
            "elements_session_client[is_aggregation_expected]" to "true",
            "elements_session_client[locale]" to Locale.getDefault().toLanguageTag(),
            "elements_session_client[mobile_session_id]" to AnalyticsRequestFactory.sessionId.toString(),
            "elements_session_client[mobile_app_id]" to context.packageName,
            "adaptive_pricing[allowed]" to adaptivePricingAllowed.toString(),
        ),
    )

    suspend fun confirm(
        id: String,
        params: ConfirmCheckoutSessionParams,
    ): Result<CheckoutSessionResponse> = executePost(
        url = confirmUrl(id),
        params = params.toParamMap().plus(Pair("elements_session_client[is_aggregation_expected]", "true")),
    )

    suspend fun detachPaymentMethod(
        sessionId: String,
        paymentMethodId: String,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "payment_method_to_detach" to paymentMethodId,
        ),
    )

    suspend fun applyPromotionCode(
        sessionId: String,
        promotionCode: String,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "promotion_code" to promotionCode,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
    )

    suspend fun updateLineItemQuantity(
        sessionId: String,
        lineItemId: String,
        quantity: Int,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "updated_line_item_quantity[line_item_id]" to lineItemId,
            "updated_line_item_quantity[quantity]" to quantity.toString(),
            "updated_line_item_quantity[fail_update_on_discount_error]" to "true",
        ),
    )

    suspend fun selectShippingRate(
        sessionId: String,
        shippingRateId: String,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "shipping_rate" to shippingRateId,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
    )

    suspend fun updateTaxRegion(
        sessionId: String,
        address: Address.State,
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
    )

    suspend fun updateTaxId(
        sessionId: String,
        type: String,
        value: String,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "tax_id_collection[tax_id][type]" to type,
            "tax_id_collection[tax_id][value]" to value,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
    )

    suspend fun updateCurrency(
        sessionId: String,
        currencyCode: String,
    ): Result<CheckoutSessionResponse> = executePost(
        url = updateUrl(sessionId),
        params = mapOf(
            "updated_currency" to currencyCode,
            "elements_session_client[is_aggregation_expected]" to "true",
        ),
    )

    private companion object {
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

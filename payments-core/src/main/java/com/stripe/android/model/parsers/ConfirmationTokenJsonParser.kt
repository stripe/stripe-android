package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmationToken

/**
 * Parser for [ConfirmationToken] JSON objects from the Stripe API.
 *
 * Handles parsing of confirmation token data including payment method previews,
 * mandate data, and shipping information.
 */
internal class ConfirmationTokenJsonParser : ModelJsonParser<ConfirmationToken> {
    override fun parse(json: org.json.JSONObject): ConfirmationToken? {
        val id = StripeJsonUtils.optString(json, FIELD_ID)
            ?: return null
        val createdTimestamp = StripeJsonUtils.optLong(json, FIELD_CREATED)
            ?: return null
        return ConfirmationToken(
            id = id,
            created = createdTimestamp,
            expiresAt = StripeJsonUtils.optLong(json, FIELD_EXPIRES_AT),
            liveMode = StripeJsonUtils.optBoolean(json, FIELD_LIVEMODE),
            mandateData = json.optJSONObject(FIELD_MANDATE_DATA)?.let {
                MandateDataJsonParser().parse(it)
            },
            paymentIntentId = StripeJsonUtils.optString(json, FIELD_PAYMENT_INTENT_ID),
            paymentMethodPreview = json.optJSONObject(FIELD_PAYMENT_METHOD_PREVIEW)?.let {
                PaymentMethodPreviewJsonParser().parse(it)
            },
            returnUrl = StripeJsonUtils.optString(json, FIELD_RETURN_URL),
            setupFutureUsage = StripeJsonUtils.optString(json, FIELD_SETUP_FUTURE_USAGE)?.let {
                    sfu ->
                ConfirmPaymentIntentParams.SetupFutureUsage.entries.find {
                    it.code == sfu
                }
            },
            setupIntentId = StripeJsonUtils.optString(json, FIELD_SETUP_INTENT_ID),
            shipping = json.optJSONObject(FIELD_SHIPPING)?.let {
                ShippingInformationJsonParser().parse(it)
            }
        )
    }

    private companion object {
        const val FIELD_ID = "id"
        const val FIELD_CREATED = "created"
        const val FIELD_EXPIRES_AT = "expires_at"
        const val FIELD_LIVEMODE = "livemode"
        const val FIELD_MANDATE_DATA = "mandate_data"
        const val FIELD_PAYMENT_INTENT_ID = "payment_intent_id"
        const val FIELD_PAYMENT_METHOD_OPTIONS = "payment_method_options"
        const val FIELD_PAYMENT_METHOD_PREVIEW = "payment_method_preview"
        const val FIELD_RETURN_URL = "return_url"
        const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        const val FIELD_SETUP_INTENT_ID = "setup_intent_id"
        const val FIELD_SHIPPING = "shipping"
    }
}

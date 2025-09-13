package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodOptions
import org.json.JSONObject

/**
 * Parser for [PaymentMethodOptions] JSON objects from the Stripe API.
 *
 * Handles parsing of payment method configuration and collected data.
 */
internal class PaymentMethodOptionsJsonParser : ModelJsonParser<PaymentMethodOptions> {
    override fun parse(json: JSONObject): PaymentMethodOptions? {
        val card = json.optJSONObject(FIELD_CARD)?.let { cardJson ->
            val cvcToken = StripeJsonUtils.optString(cardJson, FIELD_CVC_TOKEN)
            PaymentMethodOptions.Card(
                cvcToken = cvcToken
            )
        }

        return PaymentMethodOptions(
            card = card
        )
    }

    private companion object {
        const val FIELD_CARD = "card"
        const val FIELD_CVC_TOKEN = "cvc_token"
    }
}

package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodOptions
import org.json.JSONObject

class PaymentMethodOptionsJsonParser : ModelJsonParser<PaymentMethodOptions> {
    override fun parse(json: JSONObject): PaymentMethodOptions? {
        json.optJSONObject(
            FIELD_CARD
        )?.let { cardJson ->
            val cvcToken = StripeJsonUtils.optString(cardJson, FIELD_CVC_TOKEN)
            return PaymentMethodOptions(
                card = PaymentMethodOptions.Card(
                    cvcToken = cvcToken
                )
            )
        }
        return null
    }

    private companion object {
        const val FIELD_CARD = "card"
        const val FIELD_CVC_TOKEN = "cvc_token"
    }

}
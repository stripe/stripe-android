package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodMessage
import org.json.JSONObject

internal class PaymentMethodMessageJsonParser : ModelJsonParser<PaymentMethodMessage> {
    override fun parse(json: JSONObject): PaymentMethodMessage? {
        return if (!json.isNull("payment_methods")) {
            PaymentMethodMessage(json.getJSONArray("payment_methods").join(", "))
        } else null
    }
}

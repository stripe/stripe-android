package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodMessage
import org.json.JSONObject

internal class PaymentMethodMessageJsonParser : ModelJsonParser<PaymentMethodMessage> {
    override fun parse(json: JSONObject): PaymentMethodMessage? {
        val content = json.optJSONObject("content")
        val summary = content?.optJSONObject("summary")
        val message = summary?.optString("message")
        return if (!message.isNullOrBlank()) PaymentMethodMessage(message) else null
    }
}

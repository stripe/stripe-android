package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentMethodsList
import org.json.JSONArray
import org.json.JSONObject

internal class PaymentMethodsListJsonParser : ModelJsonParser<PaymentMethodsList> {
    override fun parse(json: JSONObject): PaymentMethodsList {
        val paymentMethods = runCatching {
            val data = json.optJSONArray(FIELD_DATA) ?: JSONArray()
            (0 until data.length()).mapNotNull {
                PAYMENT_METHOD_JSON_PARSER.parse(data.optJSONObject(it))
            }
        }.getOrDefault(emptyList())

        return PaymentMethodsList(paymentMethods)
    }

    private companion object {
        private const val FIELD_DATA = "data"

        private val PAYMENT_METHOD_JSON_PARSER = PaymentMethodJsonParser()
    }
}

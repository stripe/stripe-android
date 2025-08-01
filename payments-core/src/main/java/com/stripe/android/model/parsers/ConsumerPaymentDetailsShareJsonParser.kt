package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerPaymentDetailsShare
import org.json.JSONObject

internal object ConsumerPaymentDetailsShareJsonParser : ModelJsonParser<ConsumerPaymentDetailsShare> {
    private const val FIELD_PAYMENT_METHOD = "payment_method"

    override fun parse(json: JSONObject): ConsumerPaymentDetailsShare? {
        val paymentMethodJson = json.optJSONObject(FIELD_PAYMENT_METHOD) ?: return null
        val paymentMethod = PaymentMethodJsonParser().parse(paymentMethodJson)
        return ConsumerPaymentDetailsShare(paymentMethod)
    }
}

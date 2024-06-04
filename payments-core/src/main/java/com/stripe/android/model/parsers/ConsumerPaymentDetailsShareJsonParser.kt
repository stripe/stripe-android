package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerPaymentDetailsShare
import org.json.JSONObject

internal object ConsumerPaymentDetailsShareJsonParser : ModelJsonParser<ConsumerPaymentDetailsShare> {
    private const val FIELD_ID = "payment_method"

    override fun parse(json: JSONObject): ConsumerPaymentDetailsShare? {
        val id = StripeJsonUtils.optString(json, FIELD_ID) ?: return null
        return ConsumerPaymentDetailsShare(id)
    }
}

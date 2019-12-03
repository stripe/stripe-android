package com.stripe.android.model.parsers

import com.stripe.android.model.Card
import com.stripe.android.model.CustomerSource
import com.stripe.android.model.Source
import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.model.StripePaymentSource
import org.json.JSONObject

internal class CustomerSourceJsonParser : ModelJsonParser<CustomerSource?> {
    override fun parse(json: JSONObject): CustomerSource? {
        val sourceObject: StripePaymentSource? =
            when (StripeJsonUtils.optString(json, "object")) {
                Card.VALUE_CARD -> Card.fromJson(json)
                Source.VALUE_SOURCE -> Source.fromJson(json)
                else -> null
            }

        return if (sourceObject == null) {
            null
        } else {
            CustomerSource(sourceObject)
        }
    }
}

package com.stripe.android.model.parsers

import com.stripe.android.model.Card
import com.stripe.android.model.CustomerSource
import com.stripe.android.model.Source
import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.model.StripePaymentSource
import org.json.JSONObject

internal class CustomerSourceJsonParser : ModelJsonParser<CustomerSource> {
    override fun parse(json: JSONObject): CustomerSource? {
        val sourceObject: StripePaymentSource? =
            when (StripeJsonUtils.optString(json, "object")) {
                Card.OBJECT_TYPE -> CardJsonParser().parse(json)
                Source.OBJECT_TYPE -> SourceJsonParser().parse(json)
                else -> null
            }

        return if (sourceObject == null) {
            null
        } else {
            CustomerSource(sourceObject)
        }
    }
}

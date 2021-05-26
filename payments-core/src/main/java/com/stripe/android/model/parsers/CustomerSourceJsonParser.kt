package com.stripe.android.model.parsers

import com.stripe.android.model.CustomerSource
import com.stripe.android.model.StripeJsonUtils.optString
import com.stripe.android.model.StripePaymentSource
import org.json.JSONObject

internal class CustomerSourceJsonParser : ModelJsonParser<CustomerSource> {
    override fun parse(json: JSONObject): CustomerSource? {
        val sourceObject: StripePaymentSource? =
            when (optString(json, "object")) {
                "card" -> CardJsonParser().parse(json)
                "source" -> SourceJsonParser().parse(json)
                "bank_account" -> BankAccountJsonParser().parse(json)
                else -> null
            }

        return sourceObject?.let {
            CustomerSource(it)
        }
    }
}

package com.stripe.android.model.parsers

import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.StripeJsonUtils.optBoolean
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class ConsumerSessionLookupJsonParser : ModelJsonParser<ConsumerSessionLookup> {
    override fun parse(json: JSONObject): ConsumerSessionLookup? {
        val exists = optBoolean(json, FIELD_EXISTS)
        val consumerSession = ConsumerSessionJsonParser().parse(json)
        val errorMessage = optString(json, FIELD_ERROR_MESSAGE)

        return ConsumerSessionLookup(exists, consumerSession, errorMessage)
    }

    private companion object {
        private const val FIELD_EXISTS = "exists"
        private const val FIELD_ERROR_MESSAGE = "error_message"
    }
}

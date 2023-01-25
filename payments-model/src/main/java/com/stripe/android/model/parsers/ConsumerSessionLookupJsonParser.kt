package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optBoolean
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerSessionLookup
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConsumerSessionLookupJsonParser : ModelJsonParser<ConsumerSessionLookup> {
    override fun parse(json: JSONObject): ConsumerSessionLookup {
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

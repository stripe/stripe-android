package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optBoolean
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerSessionLookup
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ConsumerSessionLookupJsonParser : ModelJsonParser<ConsumerSessionLookup> {
    override fun parse(json: JSONObject): ConsumerSessionLookup {
        val exists = optBoolean(json, "exists")
        val consumerSession = ConsumerSessionJsonParser().parse(json)
        val errorMessage = optString(json, "error_message")
        val publishableKey = optString(json, "publishable_key")
        return ConsumerSessionLookup(exists, consumerSession, errorMessage, publishableKey)
    }
}

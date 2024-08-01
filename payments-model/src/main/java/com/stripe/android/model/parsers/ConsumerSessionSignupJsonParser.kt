package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerSessionSignup
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ConsumerSessionSignupJsonParser : ModelJsonParser<ConsumerSessionSignup> {

    override fun parse(json: JSONObject): ConsumerSessionSignup? {
        val consumerSession = ConsumerSessionJsonParser().parse(json)
        val publishableKey = optString(json, "publishable_key")

        return consumerSession?.let { session ->
            ConsumerSessionSignup(session, publishableKey)
        }
    }
}

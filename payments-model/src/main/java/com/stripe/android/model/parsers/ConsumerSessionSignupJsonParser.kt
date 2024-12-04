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
        val accountId = optString(json, "account_id")
        val publishableKey = optString(json, "publishable_key")

        return if (consumerSession != null && accountId != null) {
            ConsumerSessionSignup(accountId, consumerSession, publishableKey)
        } else {
            null
        }
    }
}

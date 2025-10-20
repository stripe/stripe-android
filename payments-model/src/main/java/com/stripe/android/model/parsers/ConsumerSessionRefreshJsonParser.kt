package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.LinkAuthIntent
import kotlinx.serialization.json.Json
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConsumerSessionRefreshJsonParser : ModelJsonParser<ConsumerSessionRefresh> {

    private val format = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override fun parse(json: JSONObject): ConsumerSessionRefresh {
        val consumerSession = ConsumerSessionJsonParser().parse(json)!!
        val linkAuthIntent = optString(json, FIELD_LINK_AUTH_INTENT)
            ?.let { format.decodeFromString<LinkAuthIntent>(it) }
        return ConsumerSessionRefresh(
            consumerSession = consumerSession,
            linkAuthIntent = linkAuthIntent,
        )
    }

    private companion object {
        private const val FIELD_LINK_AUTH_INTENT = "link_auth_intent"
    }
}

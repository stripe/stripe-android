package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

internal class BankConnectionsResourceLinkAccountSessionJsonParser : ModelJsonParser<BankConnectionsResourceLinkAccountSession> {
    override fun parse(json: JSONObject): BankConnectionsResourceLinkAccountSession {
        return BankConnectionsResourceLinkAccountSession(
            clientSecret = StripeJsonUtils.optString(json, FIELD_CLIENT_SECRET)!!,
            id = StripeJsonUtils.optString(json, FIELD_ID)!!
        )
    }

    private companion object {
        private const val FIELD_CLIENT_SECRET = "client_secret"
        private const val FIELD_ID = "id"
    }
}

@Parcelize
data class BankConnectionsResourceLinkAccountSession(
    val clientSecret: String,
    val id: String,
) : StripeModel

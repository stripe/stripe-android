package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.BankConnectionsLinkedAccountSession
import org.json.JSONObject

internal class BankConnectionsLinkAccountSessionJsonParser :
    ModelJsonParser<BankConnectionsLinkedAccountSession> {
    override fun parse(json: JSONObject): BankConnectionsLinkedAccountSession {
        return BankConnectionsLinkedAccountSession(
            clientSecret = json.getString(FIELD_CLIENT_SECRET),
            id = json.getString(FIELD_ID)
        )
    }

    private companion object {
        private const val FIELD_CLIENT_SECRET = "client_secret"
        private const val FIELD_ID = "id"
    }
}

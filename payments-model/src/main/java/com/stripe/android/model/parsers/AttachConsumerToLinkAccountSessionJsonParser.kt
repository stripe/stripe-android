package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.AttachConsumerToLinkAccountSession
import org.json.JSONObject

internal object AttachConsumerToLinkAccountSessionJsonParser : ModelJsonParser<AttachConsumerToLinkAccountSession> {

    override fun parse(json: JSONObject): AttachConsumerToLinkAccountSession {
        return AttachConsumerToLinkAccountSession(
            id = json.getString("id"),
            clientSecret = json.getString("client_secret"),
        )
    }
}

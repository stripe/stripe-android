package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.LinkAccountSession
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkAccountSessionJsonParser :
    ModelJsonParser<LinkAccountSession> {
    override fun parse(json: JSONObject): LinkAccountSession {
        return LinkAccountSession(
            id = json.getString(FIELD_ID),
            clientSecret = json.getString(FIELD_CLIENT_SECRET),
        )
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_CLIENT_SECRET = "client_secret"
    }
}

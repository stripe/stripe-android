package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.LinkAccountSession
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkAccountSessionJsonParser :
    ModelJsonParser<LinkAccountSession> {
    override fun parse(json: JSONObject): LinkAccountSession {
        return LinkAccountSession(
            clientSecret = json.getString(FIELD_CLIENT_SECRET),
            id = json.getString(FIELD_ID)
        )
    }

    private companion object {
        private const val FIELD_CLIENT_SECRET = "client_secret"
        private const val FIELD_ID = "id"
    }
}

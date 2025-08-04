package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.LinkAuthIntent
import com.stripe.android.model.LinkAuthIntentState
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LinkAuthIntentJsonParser : ModelJsonParser<LinkAuthIntent> {
    private const val FIELD_ID = "id"
    private const val FIELD_STATE = "state"

    override fun parse(json: JSONObject): LinkAuthIntent {
        return LinkAuthIntent(
            id = json.getString(FIELD_ID),
            state = LinkAuthIntentState.fromValue(json.getString(FIELD_STATE))
        )
    }
}

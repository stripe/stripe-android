package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.RadarSession
import org.json.JSONObject

internal class RadarSessionJsonParser : ModelJsonParser<RadarSession> {
    override fun parse(json: JSONObject): RadarSession? {
        return StripeJsonUtils.optString(json, FIELD_ID)?.let {
            RadarSession(it)
        }
    }

    private companion object {
        private const val FIELD_ID = "id"
    }
}

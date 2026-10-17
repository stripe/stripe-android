package com.stripe.android.challenge.confirmation

import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class BridgeErrorParamsJsonParser @Inject constructor() : ModelJsonParser<BridgeErrorParams> {
    override fun parse(json: JSONObject): BridgeErrorParams? {
        val message = json.optString("message").takeIf { it.isNotBlank() }
        val type = json.optString("type").takeIf { it.isNotBlank() }
        val code = json.optString("code").takeIf { it.isNotBlank() }

        return BridgeErrorParams(
            message = message,
            type = type,
            code = code
        )
    }
}

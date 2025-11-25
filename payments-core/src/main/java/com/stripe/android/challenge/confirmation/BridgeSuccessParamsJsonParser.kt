package com.stripe.android.challenge.confirmation

import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject
import javax.inject.Inject

internal class BridgeSuccessParamsJsonParser @Inject constructor() : ModelJsonParser<BridgeSuccessParams> {
    override fun parse(json: JSONObject): BridgeSuccessParams? {
        val clientSecret = json.optString("client_secret")
            .takeIf { it.isNotBlank() }
            ?: return null
        return BridgeSuccessParams(clientSecret)
    }
}

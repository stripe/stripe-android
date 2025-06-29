package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

internal class FakeECEShippingRateJsonParser : ModelJsonParser<ECEShippingRate> {
    private var result: ECEShippingRate? = null

    internal fun willReturn(result: ECEShippingRate?) {
        this.result = result
    }

    override fun parse(json: JSONObject): ECEShippingRate? {
        return result
    }
}

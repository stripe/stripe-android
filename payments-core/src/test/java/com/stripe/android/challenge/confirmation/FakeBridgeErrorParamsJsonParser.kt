package com.stripe.android.challenge.confirmation

import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

internal class FakeBridgeErrorParamsJsonParser : ModelJsonParser<BridgeErrorParams> {
    private var result: BridgeErrorParams? = null
    private var exception: Throwable? = null

    fun willReturn(result: BridgeErrorParams?) {
        this.result = result
        this.exception = null
    }

    fun willThrow(exception: Throwable) {
        this.exception = exception
        this.result = null
    }

    override fun parse(json: JSONObject): BridgeErrorParams? {
        exception?.let { throw it }
        return result
    }
}

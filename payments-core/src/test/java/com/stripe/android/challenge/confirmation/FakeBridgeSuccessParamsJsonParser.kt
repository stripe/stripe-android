package com.stripe.android.challenge.confirmation

import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

internal class FakeBridgeSuccessParamsJsonParser : ModelJsonParser<BridgeSuccessParams> {
    private var result: BridgeSuccessParams? = null
    private var exception: Throwable? = null

    fun willReturn(result: BridgeSuccessParams?) {
        this.result = result
        this.exception = null
    }

    fun willThrow(exception: Throwable) {
        this.exception = exception
        this.result = null
    }

    override fun parse(json: JSONObject): BridgeSuccessParams? {
        exception?.let { throw it }
        return result
    }
}

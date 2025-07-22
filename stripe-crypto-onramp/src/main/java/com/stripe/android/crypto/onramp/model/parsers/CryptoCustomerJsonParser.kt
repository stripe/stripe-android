package com.stripe.android.crypto.onramp.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import org.json.JSONObject

internal class CryptoCustomerJsonParser : ModelJsonParser<CryptoCustomerResponse> {

    override fun parse(json: JSONObject): CryptoCustomerResponse {
        return CryptoCustomerResponse(json.getString("id"))
    }
}

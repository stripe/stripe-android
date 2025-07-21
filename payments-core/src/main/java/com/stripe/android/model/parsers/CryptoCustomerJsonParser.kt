package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CryptoCustomerResponse
import org.json.JSONObject

internal class CryptoCustomerJsonParser : ModelJsonParser<CryptoCustomerResponse> {

    override fun parse(json: JSONObject): CryptoCustomerResponse {
        return CryptoCustomerResponse(json.getString("id"))
    }
}

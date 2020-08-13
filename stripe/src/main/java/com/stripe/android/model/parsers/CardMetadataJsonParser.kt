package com.stripe.android.model.parsers

import com.stripe.android.model.CardMetadata
import org.json.JSONArray
import org.json.JSONObject

internal class CardMetadataJsonParser(private val binPrefix: String) : ModelJsonParser<CardMetadata> {
    private val accountRangeJsonParser = AccountRangeJsonParser()

    override fun parse(json: JSONObject): CardMetadata {
        val data = json.optJSONArray("data") ?: JSONArray()
        val accountRanges =
            (0 until data.length()).mapNotNull {
                accountRangeJsonParser.parse(data.getJSONObject(it))
            }
        return CardMetadata(binPrefix, accountRanges)
    }
}

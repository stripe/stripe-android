package com.stripe.android.model.parsers

import com.stripe.android.cards.Bin
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CardMetadata
import org.json.JSONArray
import org.json.JSONObject

internal class CardMetadataJsonParser(
    private val bin: Bin,
    isNetwork: Boolean,
    accountRangeJsonParserFactory: AccountRangeJsonParser.Factory =
        AccountRangeJsonParser.Factory(::AccountRangeJsonParser)
) : ModelJsonParser<CardMetadata> {
    private val accountRangeJsonParser = accountRangeJsonParserFactory.create(isNetwork)

    override fun parse(json: JSONObject): CardMetadata {
        val data = json.optJSONArray("data") ?: JSONArray()
        val accountRanges =
            (0 until data.length()).mapNotNull {
                accountRangeJsonParser.parse(data.getJSONObject(it))
            }
        return CardMetadata(bin, accountRanges)
    }
}

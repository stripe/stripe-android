package com.stripe.android.model.parsers

import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.StripeJsonUtils.optInteger
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class CardMetadataJsonParser(private val binPrefix: String) : ModelJsonParser<CardMetadata> {
    override fun parse(json: JSONObject): CardMetadata {
        val data = json.getJSONArray("data")
        val accountRanges =
            (0 until data.length()).mapNotNull {
                val jsonEntry = data.getJSONObject(it)
                val accountRangeHigh = optString(jsonEntry, FIELD_ACCOUNT_RANGE_HIGH)
                val accountRangeLow = optString(jsonEntry, FIELD_ACCOUNT_RANGE_LOW)
                val panLength = optInteger(jsonEntry, FIELD_PAN_LENGTH)
                val brand = optString(jsonEntry, FIELD_BRAND)
                val country = optString(jsonEntry, FIELD_COUNTRY)
                if (accountRangeHigh != null &&
                    accountRangeLow != null &&
                    panLength != null &&
                    brand != null &&
                    country != null) {
                    CardMetadata.AccountRange(
                        binRange = BinRange(accountRangeLow, accountRangeHigh),
                        panLength = panLength,
                        brand = brand,
                        country = country
                    )
                } else {
                    null
                }
            }
        return CardMetadata(binPrefix, accountRanges)
    }

    private companion object {
        const val FIELD_ACCOUNT_RANGE_HIGH = "account_range_high"
        const val FIELD_ACCOUNT_RANGE_LOW = "account_range_low"
        const val FIELD_PAN_LENGTH = "pan_length"
        const val FIELD_BRAND = "brand"
        const val FIELD_COUNTRY = "country"
    }
}

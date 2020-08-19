package com.stripe.android.model.parsers

import com.stripe.android.model.BinRange
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class AccountRangeJsonParser : ModelJsonParser<CardMetadata.AccountRange> {
    override fun parse(json: JSONObject): CardMetadata.AccountRange? {
        val accountRangeHigh = StripeJsonUtils.optString(json, FIELD_ACCOUNT_RANGE_HIGH)
        val accountRangeLow = StripeJsonUtils.optString(json, FIELD_ACCOUNT_RANGE_LOW)
        val panLength = StripeJsonUtils.optInteger(json, FIELD_PAN_LENGTH)

        val brandName =
            StripeJsonUtils.optString(json, FIELD_BRAND).let { brandName ->
                CardMetadata.AccountRange.BrandInfo.values().firstOrNull { it.brandName == brandName }
            }
        return if (accountRangeHigh != null &&
            accountRangeLow != null &&
            panLength != null &&
            brandName != null) {
            CardMetadata.AccountRange(
                binRange = BinRange(accountRangeLow, accountRangeHigh),
                panLength = panLength,
                brandInfo = brandName,
                country = StripeJsonUtils.optString(json, FIELD_COUNTRY)
            )
        } else {
            null
        }
    }

    fun serialize(accountRange: CardMetadata.AccountRange): JSONObject {
        return JSONObject()
            .put(FIELD_ACCOUNT_RANGE_LOW, accountRange.binRange.low)
            .put(FIELD_ACCOUNT_RANGE_HIGH, accountRange.binRange.high)
            .put(FIELD_PAN_LENGTH, accountRange.panLength)
            .put(FIELD_BRAND, accountRange.brandInfo.brandName)
            .put(FIELD_COUNTRY, accountRange.country)
    }

    private companion object {
        const val FIELD_ACCOUNT_RANGE_HIGH = "account_range_high"
        const val FIELD_ACCOUNT_RANGE_LOW = "account_range_low"
        const val FIELD_PAN_LENGTH = "pan_length"
        const val FIELD_BRAND = "brand"
        const val FIELD_COUNTRY = "country"
    }
}

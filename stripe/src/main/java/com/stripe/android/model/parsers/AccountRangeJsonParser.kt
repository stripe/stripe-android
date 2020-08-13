package com.stripe.android.model.parsers

import com.stripe.android.model.BinRange
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class AccountRangeJsonParser : ModelJsonParser<CardMetadata.AccountRange> {
    override fun parse(json: JSONObject): CardMetadata.AccountRange? {
        val accountRangeHigh = StripeJsonUtils.optString(json, FIELD_ACCOUNT_RANGE_HIGH)
        val accountRangeLow = StripeJsonUtils.optString(json, FIELD_ACCOUNT_RANGE_LOW)
        val panLength = StripeJsonUtils.optInteger(json, FIELD_PAN_LENGTH)
        val brandName = StripeJsonUtils.optString(json, FIELD_BRAND)
        return if (accountRangeHigh != null &&
            accountRangeLow != null &&
            panLength != null &&
            brandName != null) {
            CardMetadata.AccountRange(
                binRange = BinRange(accountRangeLow, accountRangeHigh),
                panLength = panLength,
                brandName = brandName,
                brand = CardBrand.fromCode(brandName),
                country = StripeJsonUtils.optString(json, FIELD_COUNTRY)
            )
        } else {
            null
        }
    }

    private companion object {
        const val FIELD_ACCOUNT_RANGE_HIGH = "account_range_high"
        const val FIELD_ACCOUNT_RANGE_LOW = "account_range_low"
        const val FIELD_PAN_LENGTH = "pan_length"
        const val FIELD_BRAND = "brand"
        const val FIELD_COUNTRY = "country"
    }
}

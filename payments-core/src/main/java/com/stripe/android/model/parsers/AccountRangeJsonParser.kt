package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardFunding
import org.json.JSONObject

internal class AccountRangeJsonParser(
    private val isNetwork: Boolean
) : ModelJsonParser<AccountRange> {

    fun interface Factory {
        fun create(isNetwork: Boolean): AccountRangeJsonParser
    }
    override fun parse(json: JSONObject): AccountRange? {
        val accountRangeHigh = StripeJsonUtils.optString(json, FIELD_ACCOUNT_RANGE_HIGH)
        val accountRangeLow = StripeJsonUtils.optString(json, FIELD_ACCOUNT_RANGE_LOW)
        val panLength = StripeJsonUtils.optInteger(json, FIELD_PAN_LENGTH)
        val isStatic = json.optBoolean(FIELD_IS_STATIC, isNetwork.not())

        val brandInfo =
            StripeJsonUtils.optString(json, FIELD_BRAND).let { brandName ->
                AccountRange.BrandInfo.entries.firstOrNull { it.brandName == brandName }
            }
        return if (accountRangeHigh != null &&
            accountRangeLow != null &&
            panLength != null &&
            brandInfo != null
        ) {
            AccountRange(
                binRange = BinRange(accountRangeLow, accountRangeHigh, isStatic),
                panLength = panLength,
                brandInfo = brandInfo,
                country = StripeJsonUtils.optString(json, FIELD_COUNTRY),
                funding = StripeJsonUtils.optString(json, FIELD_FUNDING).let { fundingString ->
                    CardFunding.fromCode(fundingString)
                } ?: CardFunding.Unknown
            )
        } else {
            null
        }
    }

    fun serialize(accountRange: AccountRange): JSONObject {
        return JSONObject()
            .put(FIELD_ACCOUNT_RANGE_LOW, accountRange.binRange.low)
            .put(FIELD_ACCOUNT_RANGE_HIGH, accountRange.binRange.high)
            .put(FIELD_IS_STATIC, accountRange.binRange.isStatic)
            .put(FIELD_PAN_LENGTH, accountRange.panLength)
            .put(FIELD_BRAND, accountRange.brandInfo.brandName)
            .put(FIELD_COUNTRY, accountRange.country)
            .put(FIELD_FUNDING, accountRange.funding.code)
    }

    private companion object {
        const val FIELD_ACCOUNT_RANGE_HIGH = "account_range_high"
        const val FIELD_ACCOUNT_RANGE_LOW = "account_range_low"
        const val FIELD_IS_STATIC = "is_static"
        const val FIELD_PAN_LENGTH = "pan_length"
        const val FIELD_BRAND = "brand"
        const val FIELD_COUNTRY = "country"
        const val FIELD_FUNDING = "funding"
    }
}

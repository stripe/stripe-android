package com.stripe.android.model.parsers

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.SourceCardData
import com.stripe.android.model.StripeJsonUtils
import java.util.Locale
import org.json.JSONObject

internal class SourceCardDataJsonParser : ModelJsonParser<SourceCardData> {
    override fun parse(json: JSONObject): SourceCardData {
        return SourceCardData(
            addressLine1Check = StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE1_CHECK),
            addressZipCheck = StripeJsonUtils.optString(json, FIELD_ADDRESS_ZIP_CHECK),
            brand = CardBrand.fromCode(StripeJsonUtils.optString(json, FIELD_BRAND)),
            country = StripeJsonUtils.optString(json, FIELD_COUNTRY),
            cvcCheck = StripeJsonUtils.optString(json, FIELD_CVC_CHECK),
            dynamicLast4 = StripeJsonUtils.optString(json, FIELD_DYNAMIC_LAST4),
            expiryMonth = StripeJsonUtils.optInteger(json, FIELD_EXP_MONTH),
            expiryYear = StripeJsonUtils.optInteger(json, FIELD_EXP_YEAR),
            funding = Card.asFundingType(StripeJsonUtils.optString(json, FIELD_FUNDING)),
            last4 = StripeJsonUtils.optString(json, FIELD_LAST4),
            threeDSecureStatus = asThreeDSecureStatus(
                StripeJsonUtils.optString(json, FIELD_THREE_D_SECURE)
            ),
            tokenizationMethod = StripeJsonUtils.optString(json, FIELD_TOKENIZATION_METHOD)
        )
    }

    internal companion object {
        private const val FIELD_ADDRESS_LINE1_CHECK = "address_line1_check"
        private const val FIELD_ADDRESS_ZIP_CHECK = "address_zip_check"
        private const val FIELD_BRAND = "brand"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_CVC_CHECK = "cvc_check"
        private const val FIELD_DYNAMIC_LAST4 = "dynamic_last4"
        private const val FIELD_EXP_MONTH = "exp_month"
        private const val FIELD_EXP_YEAR = "exp_year"
        private const val FIELD_FUNDING = "funding"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_THREE_D_SECURE = "three_d_secure"
        private const val FIELD_TOKENIZATION_METHOD = "tokenization_method"

        @JvmSynthetic
        @VisibleForTesting
        @SourceCardData.ThreeDSecureStatus
        internal fun asThreeDSecureStatus(threeDSecureStatus: String?): String? {
            if (threeDSecureStatus == null || StripeJsonUtils.nullIfNullOrEmpty(threeDSecureStatus) == null) {
                return null
            }

            return when (threeDSecureStatus.toLowerCase(Locale.ROOT)) {
                SourceCardData.ThreeDSecureStatus.REQUIRED -> SourceCardData.ThreeDSecureStatus.REQUIRED
                SourceCardData.ThreeDSecureStatus.OPTIONAL ->
                    SourceCardData.ThreeDSecureStatus.OPTIONAL
                SourceCardData.ThreeDSecureStatus.NOT_SUPPORTED ->
                    SourceCardData.ThreeDSecureStatus.NOT_SUPPORTED
                SourceCardData.ThreeDSecureStatus.RECOMMENDED ->
                    SourceCardData.ThreeDSecureStatus.RECOMMENDED
                else -> SourceCardData.ThreeDSecureStatus.UNKNOWN
            }
        }
    }
}

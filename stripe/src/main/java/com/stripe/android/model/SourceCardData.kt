package com.stripe.android.model

import androidx.annotation.StringDef
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeJsonUtils.optInteger
import com.stripe.android.model.StripeJsonUtils.optString
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model for data contained in the SourceTypeData of a Card Source.
 */
@Parcelize
data class SourceCardData internal constructor(
    val addressLine1Check: String?,
    val addressZipCheck: String?,

    @Card.CardBrand
    @get:Card.CardBrand
    val brand: String?,

    val country: String?,
    val cvcCheck: String?,
    val dynamicLast4: String?,
    val expiryMonth: Int?,
    val expiryYear: Int?,

    @Card.FundingType
    @get:Card.FundingType
    val funding: String?,

    val last4: String?,
    @ThreeDSecureStatus
    @get:ThreeDSecureStatus
    val threeDSecureStatus: String?,

    val tokenizationMethod: String?
) : StripeSourceTypeModel() {
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(ThreeDSecureStatus.REQUIRED, ThreeDSecureStatus.OPTIONAL,
        ThreeDSecureStatus.NOT_SUPPORTED, ThreeDSecureStatus.RECOMMENDED,
        ThreeDSecureStatus.UNKNOWN)
    annotation class ThreeDSecureStatus {
        companion object {
            const val REQUIRED: String = "required"
            const val OPTIONAL: String = "optional"
            const val NOT_SUPPORTED: String = "not_supported"
            const val RECOMMENDED: String = "recommended"
            const val UNKNOWN: String = "unknown"
        }
    }

    companion object {
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

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceCardData? {
            if (jsonObject == null) {
                return null
            }

            return SourceCardData(
                addressLine1Check = optString(jsonObject, FIELD_ADDRESS_LINE1_CHECK),
                addressZipCheck = optString(jsonObject, FIELD_ADDRESS_ZIP_CHECK),
                brand = Card.asCardBrand(optString(jsonObject, FIELD_BRAND)),
                country = optString(jsonObject, FIELD_COUNTRY),
                cvcCheck = optString(jsonObject, FIELD_CVC_CHECK),
                dynamicLast4 = optString(jsonObject, FIELD_DYNAMIC_LAST4),
                expiryMonth = optInteger(jsonObject, FIELD_EXP_MONTH),
                expiryYear = optInteger(jsonObject, FIELD_EXP_YEAR),
                funding = Card.asFundingType(optString(jsonObject, FIELD_FUNDING)),
                last4 = optString(jsonObject, FIELD_LAST4),
                threeDSecureStatus = asThreeDSecureStatus(
                    optString(jsonObject, FIELD_THREE_D_SECURE)
                ),
                tokenizationMethod = optString(jsonObject, FIELD_TOKENIZATION_METHOD)
            )
        }

        @JvmSynthetic
        @VisibleForTesting
        @ThreeDSecureStatus
        internal fun asThreeDSecureStatus(threeDSecureStatus: String?): String? {
            if (StripeJsonUtils.nullIfNullOrEmpty(threeDSecureStatus) == null) {
                return null
            }

            return when {
                ThreeDSecureStatus.REQUIRED.equals(threeDSecureStatus, ignoreCase = true) ->
                    ThreeDSecureStatus.REQUIRED
                ThreeDSecureStatus.OPTIONAL.equals(threeDSecureStatus, ignoreCase = true) ->
                    ThreeDSecureStatus.OPTIONAL
                ThreeDSecureStatus.NOT_SUPPORTED.equals(threeDSecureStatus, ignoreCase = true) ->
                    ThreeDSecureStatus.NOT_SUPPORTED
                ThreeDSecureStatus.RECOMMENDED.equals(threeDSecureStatus, ignoreCase = true) ->
                    ThreeDSecureStatus.RECOMMENDED
                else -> ThreeDSecureStatus.UNKNOWN
            }
        }
    }
}

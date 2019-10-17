package com.stripe.android.model

import androidx.annotation.StringDef
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeJsonUtils.optInteger
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for data contained in the SourceTypeData of a Card Source.
 */
@Suppress("MemberVisibilityCanBePrivate")
data class SourceCardData private constructor(
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

    private class Builder : StripeSourceTypeModel.BaseBuilder() {
        var addressLine1Check: String? = null
        var addressZipCheck: String? = null
        @Card.CardBrand
        var brand: String? = null
        var country: String? = null
        var cvcCheck: String? = null
        var dynamicLast4: String? = null
        var expiryMonth: Int? = null
        var expiryYear: Int? = null
        @Card.FundingType
        var funding: String? = null
        var last4: String? = null
        @ThreeDSecureStatus
        var threeDSecureStatus: String? = null
        var tokenizationMethod: String? = null

        fun setAddressLine1Check(addressLine1Check: String?): Builder {
            this.addressLine1Check = addressLine1Check
            return this
        }

        fun setAddressZipCheck(addressZipCheck: String?): Builder {
            this.addressZipCheck = addressZipCheck
            return this
        }

        fun setBrand(brand: String?): Builder {
            this.brand = brand
            return this
        }

        fun setCountry(country: String?): Builder {
            this.country = country
            return this
        }

        fun setCvcCheck(cvcCheck: String?): Builder {
            this.cvcCheck = cvcCheck
            return this
        }

        fun setDynamicLast4(dynamicLast4: String?): Builder {
            this.dynamicLast4 = dynamicLast4
            return this
        }

        fun setExpiryMonth(expiryMonth: Int?): Builder {
            this.expiryMonth = expiryMonth
            return this
        }

        fun setExpiryYear(expiryYear: Int?): Builder {
            this.expiryYear = expiryYear
            return this
        }

        fun setFunding(funding: String?): Builder {
            this.funding = funding
            return this
        }

        fun setLast4(last4: String?): Builder {
            this.last4 = last4
            return this
        }

        fun setThreeDSecureStatus(threeDSecureStatus: String?): Builder {
            this.threeDSecureStatus = threeDSecureStatus
            return this
        }

        fun setTokenizationMethod(tokenizationMethod: String?): Builder {
            this.tokenizationMethod = tokenizationMethod
            return this
        }

        fun build(): SourceCardData {
            return SourceCardData(
                addressLine1Check = addressLine1Check,
                addressZipCheck = addressZipCheck,
                brand = brand,
                country = country,
                cvcCheck = cvcCheck,
                dynamicLast4 = dynamicLast4,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                funding = funding,
                last4 = last4,
                threeDSecureStatus = threeDSecureStatus,
                tokenizationMethod = tokenizationMethod
            )
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

        private val STANDARD_FIELDS = setOf(
            FIELD_ADDRESS_LINE1_CHECK,
            FIELD_ADDRESS_ZIP_CHECK,
            FIELD_BRAND,
            FIELD_COUNTRY,
            FIELD_CVC_CHECK,
            FIELD_DYNAMIC_LAST4,
            FIELD_EXP_MONTH,
            FIELD_EXP_YEAR,
            FIELD_FUNDING,
            FIELD_LAST4,
            FIELD_THREE_D_SECURE,
            FIELD_TOKENIZATION_METHOD
        )

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceCardData? {
            if (jsonObject == null) {
                return null
            }

            val cardData = Builder()
                .setAddressLine1Check(optString(jsonObject, FIELD_ADDRESS_LINE1_CHECK))
                .setAddressZipCheck(optString(jsonObject, FIELD_ADDRESS_ZIP_CHECK))
                .setBrand(Card.asCardBrand(optString(jsonObject, FIELD_BRAND)))
                .setCountry(optString(jsonObject, FIELD_COUNTRY))
                .setCvcCheck(optString(jsonObject, FIELD_CVC_CHECK))
                .setDynamicLast4(optString(jsonObject, FIELD_DYNAMIC_LAST4))
                .setExpiryMonth(optInteger(jsonObject, FIELD_EXP_MONTH))
                .setExpiryYear(optInteger(jsonObject, FIELD_EXP_YEAR))
                .setFunding(Card.asFundingType(optString(jsonObject, FIELD_FUNDING)))
                .setLast4(optString(jsonObject, FIELD_LAST4))
                .setThreeDSecureStatus(asThreeDSecureStatus(optString(jsonObject,
                    FIELD_THREE_D_SECURE)))
                .setTokenizationMethod(optString(jsonObject, FIELD_TOKENIZATION_METHOD))

            jsonObjectToMapWithoutKeys(jsonObject, STANDARD_FIELDS)?.let { additionalFields ->
                cardData.setAdditionalFields(additionalFields)
            }

            return cardData.build()
        }

        @JvmStatic
        @VisibleForTesting
        fun fromString(jsonString: String): SourceCardData? {
            return try {
                fromJson(JSONObject(jsonString))
            } catch (badJson: JSONException) {
                null
            }
        }

        @JvmStatic
        @VisibleForTesting
        @ThreeDSecureStatus
        fun asThreeDSecureStatus(threeDSecureStatus: String?): String? {
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

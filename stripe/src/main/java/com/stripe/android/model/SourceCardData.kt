package com.stripe.android.model

import androidx.annotation.StringDef
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeJsonUtils.optInteger
import com.stripe.android.model.StripeJsonUtils.optString
import java.util.Objects
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for data contained in the SourceTypeData of a Card Source.
 */
@Suppress("MemberVisibilityCanBePrivate")
class SourceCardData private constructor(builder: Builder) : StripeSourceTypeModel(builder) {
    val addressLine1Check: String?
    val addressZipCheck: String?

    @Card.CardBrand
    @get:Card.CardBrand
    val brand: String?

    val country: String?
    val cvcCheck: String?
    val dynamicLast4: String?
    val expiryMonth: Int?
    val expiryYear: Int?

    @Card.FundingType
    @get:Card.FundingType
    val funding: String?

    val last4: String?
    @ThreeDSecureStatus
    @get:ThreeDSecureStatus
    val threeDSecureStatus: String?

    val tokenizationMethod: String?

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(ThreeDSecureStatus.REQUIRED, ThreeDSecureStatus.OPTIONAL,
        ThreeDSecureStatus.NOT_SUPPORTED, ThreeDSecureStatus.RECOMMENDED,
        ThreeDSecureStatus.UNKNOWN)
    annotation class ThreeDSecureStatus {
        companion object {
            const val REQUIRED = "required"
            const val OPTIONAL = "optional"
            const val NOT_SUPPORTED = "not_supported"
            const val RECOMMENDED = "recommended"
            const val UNKNOWN = "unknown"
        }
    }

    init {
        addressLine1Check = builder.mAddressLine1Check
        addressZipCheck = builder.mAddressZipCheck
        brand = builder.mBrand
        country = builder.mCountry
        cvcCheck = builder.mCvcCheck
        dynamicLast4 = builder.mDynamicLast4
        expiryMonth = builder.mExpiryMonth
        expiryYear = builder.mExpiryYear
        funding = builder.mFunding
        last4 = builder.mLast4
        threeDSecureStatus = builder.mThreeDSecureStatus
        tokenizationMethod = builder.mTokenizationMethod
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return if (other is SourceCardData) {
            typedEquals(other)
        } else false
    }

    internal fun typedEquals(sourceCardData: SourceCardData): Boolean {
        return super.typedEquals(sourceCardData) &&
            addressLine1Check == sourceCardData.addressLine1Check &&
            addressZipCheck == sourceCardData.addressZipCheck &&
            brand == sourceCardData.brand &&
            country == sourceCardData.country &&
            cvcCheck == sourceCardData.cvcCheck &&
            dynamicLast4 == sourceCardData.dynamicLast4 &&
            expiryMonth == sourceCardData.expiryMonth &&
            expiryYear == sourceCardData.expiryYear &&
            funding == sourceCardData.funding &&
            last4 == sourceCardData.last4 &&
            threeDSecureStatus == sourceCardData.threeDSecureStatus &&
            tokenizationMethod == sourceCardData.tokenizationMethod
    }

    override fun hashCode(): Int {
        return Objects.hash(addressLine1Check, addressZipCheck, brand, country, cvcCheck,
            dynamicLast4, expiryMonth, expiryYear, funding, last4, threeDSecureStatus,
            tokenizationMethod)
    }

    private class Builder : StripeSourceTypeModel.BaseBuilder() {
        var mAddressLine1Check: String? = null
        var mAddressZipCheck: String? = null
        @Card.CardBrand
        var mBrand: String? = null
        var mCountry: String? = null
        var mCvcCheck: String? = null
        var mDynamicLast4: String? = null
        var mExpiryMonth: Int? = null
        var mExpiryYear: Int? = null
        @Card.FundingType
        var mFunding: String? = null
        var mLast4: String? = null
        @ThreeDSecureStatus
        var mThreeDSecureStatus: String? = null
        var mTokenizationMethod: String? = null

        fun setAddressLine1Check(addressLine1Check: String?): Builder {
            mAddressLine1Check = addressLine1Check
            return this
        }

        fun setAddressZipCheck(addressZipCheck: String?): Builder {
            mAddressZipCheck = addressZipCheck
            return this
        }

        fun setBrand(brand: String?): Builder {
            mBrand = brand
            return this
        }

        fun setCountry(country: String?): Builder {
            mCountry = country
            return this
        }

        fun setCvcCheck(cvcCheck: String?): Builder {
            mCvcCheck = cvcCheck
            return this
        }

        fun setDynamicLast4(dynamicLast4: String?): Builder {
            mDynamicLast4 = dynamicLast4
            return this
        }

        fun setExpiryMonth(expiryMonth: Int?): Builder {
            mExpiryMonth = expiryMonth
            return this
        }

        fun setExpiryYear(expiryYear: Int?): Builder {
            mExpiryYear = expiryYear
            return this
        }

        fun setFunding(funding: String?): Builder {
            mFunding = funding
            return this
        }

        fun setLast4(last4: String?): Builder {
            mLast4 = last4
            return this
        }

        fun setThreeDSecureStatus(threeDSecureStatus: String?): Builder {
            mThreeDSecureStatus = threeDSecureStatus
            return this
        }

        fun setTokenizationMethod(tokenizationMethod: String?): Builder {
            mTokenizationMethod = tokenizationMethod
            return this
        }

        fun build(): SourceCardData {
            return SourceCardData(this)
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

            val nonStandardFields = jsonObjectToMapWithoutKeys(jsonObject, STANDARD_FIELDS)
            if (nonStandardFields != null) {
                cardData.setAdditionalFields(nonStandardFields)
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
                ThreeDSecureStatus.REQUIRED.equals(threeDSecureStatus!!, ignoreCase = true) ->
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

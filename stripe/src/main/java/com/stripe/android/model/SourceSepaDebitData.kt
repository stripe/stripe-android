package com.stripe.android.model

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeJsonUtils.optString
import java.util.Objects
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for the SourceTypeData contained in a SEPA Debit Source object.
 */
class SourceSepaDebitData private constructor(builder: Builder) : StripeSourceTypeModel(builder) {

    val bankCode: String?
    val branchCode: String?
    val country: String?
    val fingerPrint: String?
    val last4: String?
    val mandateReference: String?
    val mandateUrl: String?

    init {
        bankCode = builder.mBankCode
        branchCode = builder.mBranchCode
        country = builder.mCountry
        fingerPrint = builder.mFingerPrint
        last4 = builder.mLast4
        mandateReference = builder.mMandateReference
        mandateUrl = builder.mMandateUrl
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return if (other is SourceSepaDebitData) {
            typedEquals(other)
        } else false
    }

    private fun typedEquals(obj: SourceSepaDebitData): Boolean {
        return super.typedEquals(obj) &&
            bankCode == obj.bankCode &&
            branchCode == obj.branchCode &&
            country == obj.country &&
            fingerPrint == obj.fingerPrint &&
            last4 == obj.last4 &&
            mandateReference == obj.mandateReference &&
            mandateUrl == obj.mandateUrl
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), bankCode, branchCode, country, fingerPrint,
            last4, mandateReference, mandateUrl)
    }

    private class Builder : StripeSourceTypeModel.BaseBuilder() {
        var mBankCode: String? = null
        var mBranchCode: String? = null
        var mCountry: String? = null
        var mFingerPrint: String? = null
        var mLast4: String? = null
        var mMandateReference: String? = null
        var mMandateUrl: String? = null

        internal fun setBankCode(bankCode: String?): Builder {
            mBankCode = bankCode
            return this
        }

        internal fun setBranchCode(branchCode: String?): Builder {
            mBranchCode = branchCode
            return this
        }

        internal fun setCountry(country: String?): Builder {
            mCountry = country
            return this
        }

        internal fun setFingerPrint(fingerPrint: String?): Builder {
            mFingerPrint = fingerPrint
            return this
        }

        internal fun setLast4(last4: String?): Builder {
            mLast4 = last4
            return this
        }

        internal fun setMandateReference(mandateReference: String?): Builder {
            mMandateReference = mandateReference
            return this
        }

        internal fun setMandateUrl(mandateUrl: String?): Builder {
            mMandateUrl = mandateUrl
            return this
        }

        fun build(): SourceSepaDebitData {
            return SourceSepaDebitData(this)
        }
    }

    companion object {
        private const val FIELD_BANK_CODE = "bank_code"
        private const val FIELD_BRANCH_CODE = "branch_code"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_FINGERPRINT = "fingerprint"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_MANDATE_REFERENCE = "mandate_reference"
        private const val FIELD_MANDATE_URL = "mandate_url"

        private val STANDARD_FIELDS = setOf(
            FIELD_BANK_CODE,
            FIELD_BRANCH_CODE,
            FIELD_COUNTRY,
            FIELD_FINGERPRINT,
            FIELD_LAST4,
            FIELD_MANDATE_REFERENCE,
            FIELD_MANDATE_URL
        )

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceSepaDebitData? {
            if (jsonObject == null) {
                return null
            }

            val sepaData = Builder()
                .setBankCode(optString(jsonObject, FIELD_BANK_CODE))
                .setBranchCode(optString(jsonObject, FIELD_BRANCH_CODE))
                .setCountry(optString(jsonObject, FIELD_COUNTRY))
                .setFingerPrint(optString(jsonObject, FIELD_FINGERPRINT))
                .setLast4(optString(jsonObject, FIELD_LAST4))
                .setMandateReference(optString(jsonObject, FIELD_MANDATE_REFERENCE))
                .setMandateUrl(optString(jsonObject, FIELD_MANDATE_URL))

            val nonStandardFields =
                jsonObjectToMapWithoutKeys(jsonObject, STANDARD_FIELDS)
            if (nonStandardFields != null) {
                sepaData.setAdditionalFields(nonStandardFields)
            }
            return sepaData.build()
        }

        @VisibleForTesting
        internal fun fromString(jsonString: String): SourceSepaDebitData? {
            return try {
                fromJson(JSONObject(jsonString))
            } catch (badJson: JSONException) {
                null
            }
        }
    }
}

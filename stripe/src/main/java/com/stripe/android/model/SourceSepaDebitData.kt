package com.stripe.android.model

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for the SourceTypeData contained in a SEPA Debit Source object.
 */
data class SourceSepaDebitData private constructor(
    val bankCode: String?,
    val branchCode: String?,
    val country: String?,
    val fingerPrint: String?,
    val last4: String?,
    val mandateReference: String?,
    val mandateUrl: String?
) : StripeSourceTypeModel() {

    private class Builder : StripeSourceTypeModel.BaseBuilder() {
        var bankCode: String? = null
        var branchCode: String? = null
        var country: String? = null
        var fingerPrint: String? = null
        var last4: String? = null
        var mandateReference: String? = null
        var mandateUrl: String? = null

        internal fun setBankCode(bankCode: String?): Builder {
            this.bankCode = bankCode
            return this
        }

        internal fun setBranchCode(branchCode: String?): Builder {
            this.branchCode = branchCode
            return this
        }

        internal fun setCountry(country: String?): Builder {
            this.country = country
            return this
        }

        internal fun setFingerPrint(fingerPrint: String?): Builder {
            this.fingerPrint = fingerPrint
            return this
        }

        internal fun setLast4(last4: String?): Builder {
            this.last4 = last4
            return this
        }

        internal fun setMandateReference(mandateReference: String?): Builder {
            this.mandateReference = mandateReference
            return this
        }

        internal fun setMandateUrl(mandateUrl: String?): Builder {
            this.mandateUrl = mandateUrl
            return this
        }

        fun build(): SourceSepaDebitData {
            return SourceSepaDebitData(
                bankCode = bankCode,
                branchCode = branchCode,
                country = country,
                fingerPrint = fingerPrint,
                last4 = last4,
                mandateReference = mandateReference,
                mandateUrl = mandateUrl
            )
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

package com.stripe.android.model

import com.stripe.android.model.StripeJsonUtils.optString
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model for the SourceTypeData contained in a SEPA Debit Source object.
 */
@Parcelize
data class SourceSepaDebitData internal constructor(
    val bankCode: String?,
    val branchCode: String?,
    val country: String?,
    val fingerPrint: String?,
    val last4: String?,
    val mandateReference: String?,
    val mandateUrl: String?
) : StripeSourceTypeModel() {

    companion object {
        private const val FIELD_BANK_CODE = "bank_code"
        private const val FIELD_BRANCH_CODE = "branch_code"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_FINGERPRINT = "fingerprint"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_MANDATE_REFERENCE = "mandate_reference"
        private const val FIELD_MANDATE_URL = "mandate_url"

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceSepaDebitData? {
            if (jsonObject == null) {
                return null
            }

            return SourceSepaDebitData(
                bankCode = optString(jsonObject, FIELD_BANK_CODE),
                branchCode = optString(jsonObject, FIELD_BRANCH_CODE),
                country = optString(jsonObject, FIELD_COUNTRY),
                fingerPrint = optString(jsonObject, FIELD_FINGERPRINT),
                last4 = optString(jsonObject, FIELD_LAST4),
                mandateReference = optString(jsonObject, FIELD_MANDATE_REFERENCE),
                mandateUrl = optString(jsonObject, FIELD_MANDATE_URL)
            )
        }
    }
}

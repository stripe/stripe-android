package com.stripe.android.model.parsers

import com.stripe.android.model.SourceSepaDebitData
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class SourceSepaDebitDataJsonParser : ModelJsonParser<SourceSepaDebitData> {
    override fun parse(json: JSONObject): SourceSepaDebitData {
        return SourceSepaDebitData(
            bankCode = StripeJsonUtils.optString(json, FIELD_BANK_CODE),
            branchCode = StripeJsonUtils.optString(json, FIELD_BRANCH_CODE),
            country = StripeJsonUtils.optString(json, FIELD_COUNTRY),
            fingerPrint = StripeJsonUtils.optString(json, FIELD_FINGERPRINT),
            last4 = StripeJsonUtils.optString(json, FIELD_LAST4),
            mandateReference = StripeJsonUtils.optString(json, FIELD_MANDATE_REFERENCE),
            mandateUrl = StripeJsonUtils.optString(json, FIELD_MANDATE_URL)
        )
    }

    private companion object {
        private const val FIELD_BANK_CODE = "bank_code"
        private const val FIELD_BRANCH_CODE = "branch_code"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_FINGERPRINT = "fingerprint"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_MANDATE_REFERENCE = "mandate_reference"
        private const val FIELD_MANDATE_URL = "mandate_url"
    }
}

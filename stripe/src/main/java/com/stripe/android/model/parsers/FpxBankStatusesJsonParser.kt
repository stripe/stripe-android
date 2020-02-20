package com.stripe.android.model.parsers

import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class FpxBankStatusesJsonParser : ModelJsonParser<FpxBankStatuses> {
    override fun parse(json: JSONObject): FpxBankStatuses {
        return StripeJsonUtils.optMap(json, FIELD_PARSED_BANK_STATUS)
            .takeUnless {
                it.isNullOrEmpty()
            }?.let {
                FpxBankStatuses(it as Map<String, Boolean>)
            } ?: FpxBankStatuses()
    }

    private companion object {
        private const val FIELD_PARSED_BANK_STATUS = "parsed_bank_status"
    }
}

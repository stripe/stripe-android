package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.BankStatuses
import org.json.JSONObject

internal class FpxBankStatusesJsonParser : ModelJsonParser<BankStatuses> {
    override fun parse(json: JSONObject): BankStatuses {
        return StripeJsonUtils.optMap(json, FIELD_PARSED_BANK_STATUS)
            .takeUnless {
                it.isNullOrEmpty()
            }?.let {
                BankStatuses(it as Map<String, Boolean>)
            } ?: BankStatuses()
    }

    private companion object {
        private const val FIELD_PARSED_BANK_STATUS = "parsed_bank_status"
    }
}

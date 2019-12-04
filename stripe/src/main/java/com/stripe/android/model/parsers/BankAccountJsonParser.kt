package com.stripe.android.model.parsers

import com.stripe.android.model.BankAccount
import com.stripe.android.model.BankAccount.BankAccountType
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class BankAccountJsonParser : ModelJsonParser<BankAccount> {
    override fun parse(json: JSONObject): BankAccount {
        return BankAccount(
            StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_NAME),
            asBankAccountType(
                StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_TYPE)
            ),
            StripeJsonUtils.optString(json, FIELD_BANK_NAME),
            StripeJsonUtils.optCountryCode(json, FIELD_COUNTRY),
            StripeJsonUtils.optCurrency(json, FIELD_CURRENCY),
            StripeJsonUtils.optString(json, FIELD_FINGERPRINT),
            StripeJsonUtils.optString(json, FIELD_LAST4),
            StripeJsonUtils.optString(json, FIELD_ROUTING_NUMBER)
        )
    }

    private companion object {
        private const val FIELD_ACCOUNT_HOLDER_NAME = "account_holder_name"
        private const val FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type"
        private const val FIELD_BANK_NAME = "bank_name"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_FINGERPRINT = "fingerprint"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_ROUTING_NUMBER = "routing_number"

        /**
         * Converts a String value into the appropriate [BankAccountType].
         *
         * @param possibleAccountType a String that might match a [BankAccountType] or be empty.
         * @return `null` if the input is blank or of unknown type, else the appropriate
         * [BankAccountType].
         */
        @BankAccountType
        private fun asBankAccountType(possibleAccountType: String?): String? {
            return when (possibleAccountType) {
                BankAccountType.COMPANY -> BankAccountType.COMPANY
                BankAccountType.INDIVIDUAL -> BankAccountType.INDIVIDUAL
                else -> null
            }
        }
    }
}

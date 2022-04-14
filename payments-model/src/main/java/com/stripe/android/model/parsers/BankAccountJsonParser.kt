package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.BankAccount
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BankAccountJsonParser : ModelJsonParser<BankAccount> {
    override fun parse(json: JSONObject): BankAccount {
        return BankAccount(
            id = StripeJsonUtils.optString(json, FIELD_ID),
            accountHolderName = StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_NAME),
            accountHolderType = BankAccount.Type.fromCode(
                StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_TYPE)
            ),
            bankName = StripeJsonUtils.optString(json, FIELD_BANK_NAME),
            countryCode = StripeJsonUtils.optCountryCode(json, FIELD_COUNTRY),
            currency = StripeJsonUtils.optCurrency(json, FIELD_CURRENCY),
            fingerprint = StripeJsonUtils.optString(json, FIELD_FINGERPRINT),
            last4 = StripeJsonUtils.optString(json, FIELD_LAST4),
            routingNumber = StripeJsonUtils.optString(json, FIELD_ROUTING_NUMBER),
            status = BankAccount.Status.fromCode(
                StripeJsonUtils.optString(json, FIELD_STATUS)
            )
        )
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_ACCOUNT_HOLDER_NAME = "account_holder_name"
        private const val FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type"
        private const val FIELD_BANK_NAME = "bank_name"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_FINGERPRINT = "fingerprint"
        private const val FIELD_LAST4 = "last4"
        private const val FIELD_ROUTING_NUMBER = "routing_number"
        private const val FIELD_STATUS = "status"
    }
}

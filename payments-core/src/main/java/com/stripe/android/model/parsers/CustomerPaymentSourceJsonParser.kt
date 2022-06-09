package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CustomerBankAccount
import com.stripe.android.model.CustomerCard
import com.stripe.android.model.CustomerPaymentSource
import com.stripe.android.model.CustomerSource
import org.json.JSONObject

class CustomerPaymentSourceJsonParser : ModelJsonParser<CustomerPaymentSource> {
    override fun parse(json: JSONObject): CustomerPaymentSource? {
        return when (optString(json, "object")) {
            "card" -> {
                CardJsonParser().parse(json)?.let {
                    CustomerCard(it)
                }
            }
            "source" -> {
                SourceJsonParser().parse(json)?.let {
                    CustomerSource(it)
                }
            }
            "bank_account" -> {
                BankAccountJsonParser().parse(json).let {
                    CustomerBankAccount(it)
                }
            }
            else -> null
        }
    }
}

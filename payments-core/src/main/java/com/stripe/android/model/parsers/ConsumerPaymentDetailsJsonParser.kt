package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerPaymentDetails
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConsumerPaymentDetailsJsonParser : ModelJsonParser<ConsumerPaymentDetails> {
    override fun parse(json: JSONObject): ConsumerPaymentDetails {
        val paymentDetails = json.optJSONArray(FIELD_PAYMENT_DETAILS)?.let { paymentDetailsArray ->
            (0 until paymentDetailsArray.length())
                .map { index -> paymentDetailsArray.getJSONObject(index) }
                .mapNotNull { parsePaymentDetails(it) }
        } // Response with a single object might have it not in a list
            ?: json.optJSONObject(FIELD_PAYMENT_DETAILS)?.let {
                parsePaymentDetails(it)
            }?.let { listOf(it) } ?: emptyList()
        return ConsumerPaymentDetails(paymentDetails)
    }

    private fun parsePaymentDetails(json: JSONObject): ConsumerPaymentDetails.PaymentDetails? =
        optString(json, FIELD_TYPE)?.let {
            when (it.lowercase()) {
                ConsumerPaymentDetails.Card.type -> {
                    val cardDetails = json.getJSONObject(FIELD_CARD_DETAILS)

                    ConsumerPaymentDetails.Card(
                        json.getString(FIELD_ID),
                        cardDetails.getString(FIELD_CARD_LAST_4),
                    )
                }
                ConsumerPaymentDetails.BankAccount.type -> {
                    val bankAccountDetails = json.getJSONObject(FIELD_BANK_ACCOUNT_DETAILS)
                    ConsumerPaymentDetails.BankAccount(
                        json.getString(FIELD_ID),
                        optString(bankAccountDetails, FIELD_BANK_ACCOUNT_BANK_ICON_CODE),
                        bankAccountDetails.getString(FIELD_BANK_ACCOUNT_BANK_NAME),
                        bankAccountDetails.getString(FIELD_BANK_ACCOUNT_LAST_4)
                    )
                }
                else -> null
            }
        }

    private companion object {
        private const val FIELD_PAYMENT_DETAILS = "redacted_payment_details"

        private const val FIELD_TYPE = "type"
        private const val FIELD_ID = "id"

        private const val FIELD_CARD_DETAILS = "card_details"
        private const val FIELD_CARD_LAST_4 = "last4"

        private const val FIELD_BANK_ACCOUNT_DETAILS = "bank_account_details"
        private const val FIELD_BANK_ACCOUNT_BANK_ICON_CODE = "bank_icon_code"
        private const val FIELD_BANK_ACCOUNT_BANK_NAME = "bank_name"
        private const val FIELD_BANK_ACCOUNT_LAST_4 = "last4"
    }
}

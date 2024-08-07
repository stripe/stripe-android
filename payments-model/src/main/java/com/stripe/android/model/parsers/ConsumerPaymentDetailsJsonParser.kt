package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerPaymentDetails2
import org.json.JSONObject

private const val FIELD_PAYMENT_DETAILS = "redacted_payment_details"
private const val FIELD_TYPE = "type"
private const val FIELD_ID = "id"
private const val FIELD_CARD_DETAILS = "card_details"
private const val FIELD_CARD_LAST_4 = "last4"
private const val FIELD_BANK_ACCOUNT_DETAILS = "bank_account_details"
private const val FIELD_BANK_ACCOUNT_LAST_4 = "last4"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ConsumerPaymentDetailsJsonParser : ModelJsonParser<ConsumerPaymentDetails2> {

    override fun parse(json: JSONObject): ConsumerPaymentDetails2 {
        val paymentDetails = json.optJSONArray(FIELD_PAYMENT_DETAILS)?.let { paymentDetailsArray ->
            (0 until paymentDetailsArray.length())
                .map { index -> paymentDetailsArray.getJSONObject(index) }
                .mapNotNull { parsePaymentDetails(it) }
        } // Response with a single object might have it not in a list
            ?: json.optJSONObject(FIELD_PAYMENT_DETAILS)?.let {
                parsePaymentDetails(it)
            }?.let { listOf(it) } ?: emptyList()
        return ConsumerPaymentDetails2(paymentDetails)
    }

    private fun parsePaymentDetails(json: JSONObject): ConsumerPaymentDetails2.PaymentDetails? =
        optString(json, FIELD_TYPE)?.let {
            when (it.lowercase()) {
                "card" -> {
                    val cardDetails = json.getJSONObject(FIELD_CARD_DETAILS)
                    ConsumerPaymentDetails2.Card(
                        json.getString(FIELD_ID),
                        cardDetails.getString(FIELD_CARD_LAST_4),
                    )
                }
                "bank_account" -> {
                    val bankAccountDetails = json.getJSONObject(FIELD_BANK_ACCOUNT_DETAILS)
                    ConsumerPaymentDetails2.BankAccount(
                        json.getString(FIELD_ID),
                        bankAccountDetails.getString(FIELD_BANK_ACCOUNT_LAST_4)
                    )
                }
                else -> null
            }
        }
}

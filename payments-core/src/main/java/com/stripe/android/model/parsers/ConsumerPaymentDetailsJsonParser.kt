package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import org.json.JSONObject

internal class ConsumerPaymentDetailsJsonParser : ModelJsonParser<ConsumerPaymentDetails> {
    override fun parse(json: JSONObject): ConsumerPaymentDetails {
        val paymentDetails = json.optJSONArray(FIELD_PAYMENT_DETAILS)
            ?.let { paymentDetailsArray ->
                (0 until paymentDetailsArray.length())
                    .map { index -> paymentDetailsArray.getJSONObject(index) }
                    .mapNotNull { parsePaymentDetails(it) }
            } ?: emptyList()
        return ConsumerPaymentDetails(paymentDetails)
    }

    private fun parsePaymentDetails(json: JSONObject): ConsumerPaymentDetails.PaymentDetails? =
        optString(json, FIELD_TYPE)?.let {
            when (it.lowercase()) {
                ConsumerPaymentDetails.Card.type -> {
                    val cardDetails = json.getJSONObject(FIELD_CARD_DETAILS)
                    ConsumerPaymentDetails.Card(
                        json.getString(FIELD_ID),
                        json.getBoolean(FIELD_IS_DEFAULT),
                        cardDetails.getInt(FIELD_CARD_EXPIRY_YEAR),
                        cardDetails.getInt(FIELD_CARD_EXPIRY_MONTH),
                        CardBrand.fromCode(cardDetails.getString(FIELD_CARD_BRAND)),
                        cardDetails.getString(FIELD_CARD_LAST_4)
                    )
                }
                else -> null
            }
        }

    private companion object {
        private const val FIELD_PAYMENT_DETAILS = "redacted_payment_details"

        private const val FIELD_TYPE = "type"
        private const val FIELD_ID = "id"
        private const val FIELD_IS_DEFAULT = "is_default"

        private const val FIELD_CARD_DETAILS = "card_details"
        private const val FIELD_CARD_EXPIRY_YEAR = "exp_year"
        private const val FIELD_CARD_EXPIRY_MONTH = "exp_month"
        private const val FIELD_CARD_BRAND = "brand"
        private const val FIELD_CARD_LAST_4 = "last4"
    }
}

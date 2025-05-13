package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject

internal object PaymentMethodWithLinkDetailsJsonParser : ModelJsonParser<PaymentMethod> {

    override fun parse(json: JSONObject): PaymentMethod? {
        val paymentMethod = PaymentMethodJsonParser().parse(json.getJSONObject("payment_method"))
        val linkPaymentDetailsJson = json.optJSONObject("link_payment_details")

        if (isUnsupportedLinkPaymentDetailsType(linkPaymentDetailsJson)) {
            // This is a Link payment method, but we don't support the type yet. We can't render them, so hide them.
            return null
        }

        val consumerPaymentDetails = linkPaymentDetailsJson?.let {
            ConsumerPaymentDetailsJsonParser.parsePaymentDetails(it)
        }

        val linkDetails = when (consumerPaymentDetails) {
            is ConsumerPaymentDetails.Card -> {
                LinkPaymentDetails(
                    expMonth = consumerPaymentDetails.expiryMonth,
                    expYear = consumerPaymentDetails.expiryYear,
                    last4 = consumerPaymentDetails.last4,
                    brand = consumerPaymentDetails.brand,
                )
            }
            is ConsumerPaymentDetails.BankAccount,
            is ConsumerPaymentDetails.Passthrough,
            null -> {
                null
            }
        }

        // TODO(tillh-stripe): This is a short-term solution. We plan to create a new type that
        //  contains payment method and Link information, but we can't easily do that right now.
        return paymentMethod.copy(
            linkPaymentDetails = linkDetails,
        )
    }

    private fun isUnsupportedLinkPaymentDetailsType(json: JSONObject?): Boolean {
        return json != null && optString(json, "type") != "CARD"
    }
}

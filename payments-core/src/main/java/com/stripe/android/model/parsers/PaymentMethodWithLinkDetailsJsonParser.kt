package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.optBoolean
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
        val isLinkOrigin = optBoolean(json, "is_link_origin")

        if (isUnsupportedLinkPaymentDetailsType(linkPaymentDetailsJson)) {
            // This is a Link payment method, but we don't support the type yet. We can't render them, so hide them.
            return null
        }

        val consumerPaymentDetails = linkPaymentDetailsJson?.let {
            ConsumerPaymentDetailsJsonParser.parsePaymentDetails(it)
        }

        val linkPaymentDetails = when (consumerPaymentDetails) {
            is ConsumerPaymentDetails.Card -> {
                LinkPaymentDetails.Card(
                    nickname = consumerPaymentDetails.nickname,
                    expMonth = consumerPaymentDetails.expiryMonth,
                    expYear = consumerPaymentDetails.expiryYear,
                    last4 = consumerPaymentDetails.last4,
                    brand = consumerPaymentDetails.brand,
                    funding = consumerPaymentDetails.funding,
                )
            }
            is ConsumerPaymentDetails.BankAccount -> {
                LinkPaymentDetails.BankAccount(
                    bankName = consumerPaymentDetails.bankName,
                    last4 = consumerPaymentDetails.last4,
                )
            }
            null -> {
                null
            }
        }

        // TODO(tillh-stripe): This is a short-term solution. We plan to create a new type that
        //  contains payment method and Link information, but we can't easily do that right now.
        return paymentMethod.copy(
            linkPaymentDetails = linkPaymentDetails,
            // A payment method is in passthrough mode if it has Link origin but no link details
            isLinkPassthroughMode = isLinkOrigin && linkPaymentDetails == null,
        )
    }

    private fun isUnsupportedLinkPaymentDetailsType(json: JSONObject?): Boolean {
        val supportedTypes = setOf("CARD", "BANK_ACCOUNT")
        return json != null && optString(json, "type") !in supportedTypes
    }
}

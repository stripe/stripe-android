package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject

internal object PaymentMethodWithLinkDetailsJsonParser : ModelJsonParser<PaymentMethod> {

    override fun parse(json: JSONObject): PaymentMethod? {
        val paymentMethod = PaymentMethodJsonParser().parse(json.getJSONObject("payment_method"))

        val consumerPaymentDetails = if (FeatureFlags.linkPMsInSPM.isEnabled) {
            json.optJSONObject("link_payment_details")?.let {
                ConsumerPaymentDetailsJsonParser.parsePaymentDetails(it)
            }
        } else {
            null
        }

        val cardDetails = consumerPaymentDetails as? ConsumerPaymentDetails.Card

        val linkDetails = cardDetails?.let {
            LinkPaymentDetails(
                expMonth = it.expiryMonth,
                expYear = it.expiryYear,
                last4 = it.last4,
                brand = it.brand,
            )
        }

        // TODO(tillh-stripe): This is a short-term solution. We plan to create a new type that
        //  contains payment method and Link information, but we can't easily do that right now.
        return paymentMethod.copy(
            linkPaymentDetails = linkDetails,
        )
    }
}

package com.stripe.android.link.confirmation

import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails

internal class FakeLinkConfirmationHandler : LinkConfirmationHandler {
    var confirmResult: Result = Result.Succeeded(
        intent = TestFactory.LINK_CONFIGURATION.stripeIntent,
        checkoutSessionResponse = null,
    )
    var confirmWithLinkPaymentDetailsResult: Result = Result.Succeeded(
        intent = TestFactory.LINK_CONFIGURATION.stripeIntent,
        checkoutSessionResponse = null,
    )
    val calls = arrayListOf<Call>()
    val confirmWithLinkPaymentDetailsCall = arrayListOf<ConfirmWithLinkPaymentDetailsCall>()

    override suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): Result {
        calls.add(
            element = Call(
                paymentDetails = paymentDetails,
                linkAccount = linkAccount,
                cvc = cvc
            )
        )
        return confirmResult
    }

    override suspend fun confirm(
        paymentDetails: LinkPaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): Result {
        confirmWithLinkPaymentDetailsCall.add(
            element = ConfirmWithLinkPaymentDetailsCall(
                paymentDetails = paymentDetails,
                linkAccount = linkAccount,
                cvc = cvc
            )
        )
        return confirmWithLinkPaymentDetailsResult
    }

    data class Call(
        val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        val linkAccount: LinkAccount,
        val cvc: String?
    )

    data class ConfirmWithLinkPaymentDetailsCall(
        val paymentDetails: LinkPaymentDetails,
        val linkAccount: LinkAccount,
        val cvc: String?
    )
}

package com.stripe.android.link.confirmation

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails

internal class FakeLinkConfirmationHandler : LinkConfirmationHandler {
    var confirmResult: Result = Result.Succeeded
    val calls = arrayListOf<Call>()

    override suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?
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

    data class Call(
        val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        val linkAccount: LinkAccount,
        val cvc: String?
    )
}

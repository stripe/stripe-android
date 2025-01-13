package com.stripe.android.link.confirmation

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails

internal class FakeLinkConfirmationHandler : LinkConfirmationHandler {
    var confirmResult: Result = Result.Succeeded

    override suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?
    ) = confirmResult
}

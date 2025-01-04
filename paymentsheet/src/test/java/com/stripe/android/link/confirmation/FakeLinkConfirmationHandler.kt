package com.stripe.android.link.confirmation

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails

internal open class FakeLinkConfirmationHandler : LinkConfirmationHandler {
    var result: Result = Result.Succeeded
    override suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount
    ) = result
}

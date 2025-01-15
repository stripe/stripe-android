package com.stripe.android.link.confirmation

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakeLinkConfirmationHandler : LinkConfirmationHandler {
    var confirmResult: Result = Result.Succeeded
    val calls = arrayListOf<Call>()

    override suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?
    ): Result {
        calls.add(
            element = Call.WithPaymentDetails(
                paymentDetails = paymentDetails,
                linkAccount = linkAccount,
                cvc = cvc
            )
        )
        return confirmResult
    }

    override suspend fun confirm(
        paymentSelection: PaymentSelection,
        linkAccount: LinkAccount
    ): Result {
        calls.add(
            element = Call.WithPaymentSelection(
                paymentSelection = paymentSelection,
                linkAccount = linkAccount
            )
        )
        return confirmResult
    }

    sealed interface Call {
        data class WithPaymentDetails(
            val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
            val linkAccount: LinkAccount,
            val cvc: String?
        ) : Call

        data class WithPaymentSelection(
            val paymentSelection: PaymentSelection,
            val linkAccount: LinkAccount,
        ) : Call
    }
}

package com.stripe.android.link.confirmation

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler

internal interface LinkConfirmationHandler {
    suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount
    ): Result

    fun interface Factory {
        fun create(confirmationHandler: ConfirmationHandler): LinkConfirmationHandler
    }
}

internal sealed interface Result {
    data object Succeeded : Result
    data object Canceled : Result
    data class Failed(val message: ResolvableString) : Result
}

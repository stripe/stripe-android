package com.stripe.android.link.confirmation

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler

internal interface LinkConfirmationHandler {
    suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): Result

    suspend fun confirm(
        paymentDetails: LinkPaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): Result

    fun interface Factory {
        fun create(confirmationHandler: ConfirmationHandler): LinkConfirmationHandler
    }
}

internal sealed interface Result {
    val confirmationRequestId: String?

    data class Succeeded(
        override val confirmationRequestId: String?,
    ) : Result

    data class Canceled(
        override val confirmationRequestId: String?,
    ) : Result

    data class Failed(
        val message: ResolvableString,
        override val confirmationRequestId: String?,
    ) : Result
}

package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.parcelize.Parcelize

internal sealed interface CheckoutSessionConfirmationOption : ConfirmationHandler.Option {
    @Parcelize
    data class WithoutPaymentMethod(val email: String?) : CheckoutSessionConfirmationOption

    @Parcelize
    data class Finalize(
        val response: CheckoutSessionResponse,
        val intent: StripeIntent,
    ) : CheckoutSessionConfirmationOption
}

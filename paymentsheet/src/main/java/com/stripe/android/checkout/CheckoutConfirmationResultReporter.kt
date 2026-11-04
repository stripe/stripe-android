package com.stripe.android.checkout

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

/**
 * Reports the final outcome of a Checkout confirmation, after any post-confirmation polling.
 */
internal fun interface CheckoutConfirmationResultReporter {
    fun reportConfirmationResult(
        result: ConfirmationHandler.Result,
        response: CheckoutSessionResponse?,
    )
}

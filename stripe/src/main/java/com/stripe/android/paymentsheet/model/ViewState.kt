package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult

sealed class ViewState {
    // Might consider this just taking a string for the button
    data class Ready(
        val append: String
    ) : ViewState()

    object Confirming : ViewState()

    // Might consider removing paymentIntentResult from the button
    data class Completed(
        val paymentIntentResult: PaymentIntentResult
    ) : ViewState()
}

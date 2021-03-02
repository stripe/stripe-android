package com.stripe.android.paymentsheet.model

sealed class ViewState {
    // Might consider this just taking a string for the button
    data class Ready(
        val append: String
    ) : ViewState()

    object Confirming : ViewState()

    data class Completed<T>(
        val result: T
    ) : ViewState()
}

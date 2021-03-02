package com.stripe.android.paymentsheet.model

sealed class ViewState {
    data class Ready(
        val append: String
    ) : ViewState()

    object Confirming : ViewState()

    data class Completed<T>(
        val result: T
    ) : ViewState()
}

package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection

/** TODO: Rename to ViewState if can get in the right package */
sealed class AddButtonViewState {
    object Ready : AddButtonViewState()

    object Confirming : AddButtonViewState()

    data class Completed(
            val paymentSelection: PaymentSelection
    ) : AddButtonViewState()
}
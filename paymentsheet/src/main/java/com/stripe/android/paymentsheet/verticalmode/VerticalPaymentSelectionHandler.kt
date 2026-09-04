package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface VerticalPaymentSelectionHandler {
    fun select(selection: PaymentSelection, isUserInput: Boolean)

    fun onSelectionComplete()
}

internal class ImmediateVerticalPaymentSelectionHandler(
    private val updateSelection: (PaymentSelection, Boolean) -> Unit,
    private val completionAction: (() -> Unit)?,
) : VerticalPaymentSelectionHandler {
    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        updateSelection(selection, isUserInput)
        onSelectionComplete()
    }

    override fun onSelectionComplete() {
        completionAction?.invoke()
    }
}

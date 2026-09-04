package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun interface VerticalPaymentSelectionHandler {
    fun select(selection: PaymentSelection, isUserInput: Boolean)
}

internal class ImmediateVerticalPaymentSelectionHandler(
    private val updateSelection: (PaymentSelection, Boolean) -> Unit,
    private val onSelectionComplete: () -> Unit,
) : VerticalPaymentSelectionHandler {
    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        updateSelection(selection, isUserInput)
        onSelectionComplete()
    }
}

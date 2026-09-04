package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun interface VerticalSavedPaymentMethodSelectionHandler {
    fun select(selection: PaymentSelection.Saved)
}

internal class ImmediateVerticalSavedPaymentMethodSelectionHandler(
    private val updateSelection: (PaymentSelection.Saved) -> Unit,
    private val onSelectionComplete: () -> Unit,
) : VerticalSavedPaymentMethodSelectionHandler {
    override fun select(selection: PaymentSelection.Saved) {
        updateSelection(selection)
        onSelectionComplete()
    }
}

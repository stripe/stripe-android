package com.stripe.android.paymentelement.embedded

import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject

internal interface EmbeddedSavedPaymentMethodSelectionHandler {
    fun select(selection: PaymentSelection.Saved)
}

internal class DefaultEmbeddedSavedPaymentMethodSelectionHandler @Inject constructor(
    private val selectionHolder: EmbeddedSelectionHolder,
    private val immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
) : EmbeddedSavedPaymentMethodSelectionHandler {
    override fun select(selection: PaymentSelection.Saved) {
        selectionHolder.setSelection(selection)
        immediateActionHandler.invoke()
    }
}

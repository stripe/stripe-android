package com.stripe.android.paymentelement.embedded

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal interface EmbeddedSavedPaymentMethodSelectionHandler {
    val pendingSelection: StateFlow<PaymentSelection.Saved?>
    val error: StateFlow<Throwable?>

    fun select(selection: PaymentSelection.Saved)
}

internal class DefaultEmbeddedSavedPaymentMethodSelectionHandler @Inject constructor(
    private val selectionHolder: EmbeddedSelectionHolder,
    private val immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
) : EmbeddedSavedPaymentMethodSelectionHandler {
    override val pendingSelection = stateFlowOf<PaymentSelection.Saved?>(null)
    override val error = stateFlowOf<Throwable?>(null)

    override fun select(selection: PaymentSelection.Saved) {
        selectionHolder.setSelection(selection)
        immediateActionHandler.invoke()
    }
}

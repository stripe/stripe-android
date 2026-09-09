package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedVerticalProcessing
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CheckoutPaymentSelectionHandler @Inject constructor(
    private val savedPaymentMethodSelector: CheckoutSavedPaymentMethodSelector,
    @EmbeddedVerticalProcessing private val processing: StateFlow<Boolean>,
    selectionHolder: EmbeddedSelectionHolder,
    immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : VerticalPaymentSelectionHandler {
    private val immediateHandler = ImmediateVerticalPaymentSelectionHandler(
        updateSelection = { selection, _ -> selectionHolder.setSelection(selection) },
        completionAction = immediateActionHandler::invoke,
    )

    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        when (selection) {
            is PaymentSelection.Saved -> selectSavedPaymentMethod(selection)
            else -> immediateHandler.select(selection, isUserInput)
        }
    }

    override fun onSelectionComplete() {
        immediateHandler.onSelectionComplete()
    }

    private fun selectSavedPaymentMethod(selection: PaymentSelection.Saved) {
        if (processing.value) return

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            savedPaymentMethodSelector.select(selection).onSuccess {
                onSelectionComplete()
            }
        }
    }
}

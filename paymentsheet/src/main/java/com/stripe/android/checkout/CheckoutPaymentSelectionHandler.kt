package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentSelectionHandler @Inject constructor(
    private val checkoutController: CheckoutController,
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
        if (checkoutController.isUpdating.value) return

        // Admit the controller mutation before select() returns so another selection observes it.
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            checkoutController.selectSavedPaymentMethod(selection).onSuccess {
                onSelectionComplete()
            }
        }
    }
}

package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentSelectionHandler @Inject constructor(
    private val checkoutController: CheckoutController,
    selectionHolder: EmbeddedSelectionHolder,
    immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : VerticalPaymentSelectionHandler {
    private val _state = MutableStateFlow<VerticalPaymentSelectionHandler.State>(
        VerticalPaymentSelectionHandler.State.Idle
    )
    override val state = _state.asStateFlow()

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
        if (_state.value is VerticalPaymentSelectionHandler.State.Selecting) return

        _state.value = VerticalPaymentSelectionHandler.State.Selecting(selection)

        coroutineScope.launch {
            try {
                checkoutController.selectSavedPaymentMethod(selection).onSuccess {
                    onSelectionComplete()
                }
            } finally {
                _state.value = VerticalPaymentSelectionHandler.State.Idle
            }
        }
    }
}

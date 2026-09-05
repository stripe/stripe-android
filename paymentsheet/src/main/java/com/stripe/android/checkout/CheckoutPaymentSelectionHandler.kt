package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
internal class CheckoutPaymentSelectionHandler @Inject constructor(
    private val checkoutController: CheckoutController,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : VerticalPaymentSelectionHandler {
    private val _state = MutableStateFlow<VerticalPaymentSelectionHandler.State>(
        VerticalPaymentSelectionHandler.State.Idle
    )
    override val state = _state.asStateFlow()

    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        when (selection) {
            is PaymentSelection.Saved -> selectSavedPaymentMethod(selection)
            else -> {
                selectionHolder.setSelection(selection)
                onSelectionComplete()
            }
        }
    }

    override fun onSelectionComplete() {
        immediateActionHandler.invoke()
    }

    override fun clearFailure() {
        if (_state.value is VerticalPaymentSelectionHandler.State.Failed) {
            _state.value = VerticalPaymentSelectionHandler.State.Idle
        }
    }

    private fun selectSavedPaymentMethod(selection: PaymentSelection.Saved) {
        if (_state.value is VerticalPaymentSelectionHandler.State.Selecting) return

        _state.value = VerticalPaymentSelectionHandler.State.Selecting(selection)
        coroutineScope.launch {
            checkoutController.selectSavedPaymentMethod(selection).fold(
                onSuccess = {
                    onSelectionComplete()
                    _state.value = VerticalPaymentSelectionHandler.State.Idle
                },
                onFailure = { error ->
                    _state.value = VerticalPaymentSelectionHandler.State.Failed(error)
                },
            )
        }
    }
}

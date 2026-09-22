package com.stripe.android.checkout

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
@Singleton
internal class CheckoutPaymentSelectionHandler @Inject constructor(
    private val checkoutController: CheckoutController,
    selectionHolder: EmbeddedSelectionHolder,
    immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : VerticalPaymentSelectionHandler {
    private val _state = MutableStateFlow<SavedPaymentMethodSelectionState>(
        SavedPaymentMethodSelectionState.Idle
    )
    val state: StateFlow<SavedPaymentMethodSelectionState> = _state.asStateFlow()

    private val immediateHandler = ImmediateVerticalPaymentSelectionHandler(
        updateSelection = { selection, _ ->
            selectionHolder.setSelection(selection)
            clearError()
        },
        completionAction = immediateActionHandler::invoke,
    )

    init {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            selectionHolder.selection.drop(1).collect {
                clearError()
            }
        }
    }

    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        when (selection) {
            is PaymentSelection.Saved -> selectSavedPaymentMethod(selection)
            else -> immediateHandler.select(selection, isUserInput)
        }
    }

    override fun onSelectionComplete() {
        clearError()
        immediateHandler.onSelectionComplete()
    }

    private fun selectSavedPaymentMethod(selection: PaymentSelection.Saved) {
        if (state.value is SavedPaymentMethodSelectionState.Pending) return

        _state.value = SavedPaymentMethodSelectionState.Pending
        coroutineScope.launch {
            try {
                checkoutController.selectSavedPaymentMethod(selection).fold(
                    onSuccess = { onSelectionComplete() },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        _state.value = SavedPaymentMethodSelectionState.Failed(error)
                    },
                )
            } finally {
                _state.update { state ->
                    if (state is SavedPaymentMethodSelectionState.Pending) {
                        SavedPaymentMethodSelectionState.Idle
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun clearError() {
        _state.update { state ->
            if (state is SavedPaymentMethodSelectionState.Failed) {
                SavedPaymentMethodSelectionState.Idle
            } else {
                state
            }
        }
    }
}

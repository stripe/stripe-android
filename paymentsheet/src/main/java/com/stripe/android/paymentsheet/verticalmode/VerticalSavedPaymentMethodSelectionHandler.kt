package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal interface VerticalSavedPaymentMethodSelectionHandler {
    val state: StateFlow<State>

    fun select(selection: PaymentSelection.Saved)

    fun clearFailure()

    sealed interface State {
        data object Idle : State
        data class Selecting(val selection: PaymentSelection.Saved) : State
        data class Failed(val error: Throwable) : State
    }
}

internal class ImmediateVerticalSavedPaymentMethodSelectionHandler(
    private val updateSelection: (PaymentSelection.Saved) -> Unit,
    private val onSelectionComplete: () -> Unit,
) : VerticalSavedPaymentMethodSelectionHandler {
    override val state = stateFlowOf<VerticalSavedPaymentMethodSelectionHandler.State>(
        VerticalSavedPaymentMethodSelectionHandler.State.Idle
    )

    override fun select(selection: PaymentSelection.Saved) {
        updateSelection(selection)
        onSelectionComplete()
    }

    override fun clearFailure() = Unit
}

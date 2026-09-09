package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal interface VerticalPaymentSelectionHandler {
    val state: StateFlow<State>

    fun select(selection: PaymentSelection, isUserInput: Boolean)

    fun onSelectionComplete()

    sealed interface State {
        data object Idle : State
        data class Selecting(val selection: PaymentSelection.Saved) : State
    }
}

internal class ImmediateVerticalPaymentSelectionHandler(
    private val updateSelection: (PaymentSelection, Boolean) -> Unit,
    private val completionAction: (() -> Unit)?,
) : VerticalPaymentSelectionHandler {
    override val state = stateFlowOf<VerticalPaymentSelectionHandler.State>(
        VerticalPaymentSelectionHandler.State.Idle
    )

    override fun select(selection: PaymentSelection, isUserInput: Boolean) {
        updateSelection(selection, isUserInput)
        onSelectionComplete()
    }

    override fun onSelectionComplete() {
        completionAction?.invoke()
    }
}

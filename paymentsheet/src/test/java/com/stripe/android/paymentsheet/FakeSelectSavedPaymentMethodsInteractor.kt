package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.ui.SelectSavedPaymentMethodsInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeSelectSavedPaymentMethodsInteractor(
    initialState: SelectSavedPaymentMethodsInteractor.State = SelectSavedPaymentMethodsInteractor.State(
        paymentOptionsItems = emptyList(),
        selectedPaymentOptionsItem = null,
        isEditing = false,
        isProcessing = false,
        canEdit = false,
        canRemove = false,
    ),
    private val viewActionRecorder: ViewActionRecorder<SelectSavedPaymentMethodsInteractor.ViewAction> =
        ViewActionRecorder(),
) : SelectSavedPaymentMethodsInteractor {
    override val isLiveMode: Boolean = true

    override val state: StateFlow<SelectSavedPaymentMethodsInteractor.State> = stateFlowOf(initialState)

    override fun handleViewAction(viewAction: SelectSavedPaymentMethodsInteractor.ViewAction) {
        viewActionRecorder.record(viewAction)
    }

    override fun close() {
        // Do nothing.
    }
}

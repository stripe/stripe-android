package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.flow.StateFlow

internal interface EmbeddedSavedPaymentMethodSelector {
    val selectionState: StateFlow<SavedPaymentMethodSelectionState>

    suspend fun select(selection: PaymentSelection.Saved): Result<Unit>
}

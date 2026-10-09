package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.flow.StateFlow

internal interface ManageScreenSavedPaymentMethodSelector {
    val selectionState: StateFlow<SavedPaymentMethodSelectionState>
    val checkoutSessionResponse: CheckoutSessionResponse?

    suspend fun select(selection: PaymentSelection.Saved): Result<Unit>

    suspend fun syncBillingAfterEdit(original: PaymentMethod, updated: PaymentMethod): Result<Unit>

    fun clearError()
}

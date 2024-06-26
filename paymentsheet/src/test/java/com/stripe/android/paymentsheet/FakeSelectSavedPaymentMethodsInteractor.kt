package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.ui.SelectSavedPaymentMethodsInteractor
import kotlinx.coroutines.flow.StateFlow
import org.mockito.Mockito.mock

internal object FakeSelectSavedPaymentMethodsInteractor : SelectSavedPaymentMethodsInteractor {
    override val state: StateFlow<SelectSavedPaymentMethodsInteractor.State> = mock()

    override fun handleViewAction(viewAction: SelectSavedPaymentMethodsInteractor.ViewAction) {
        // Do nothing.
    }

    override fun close() {
        // Do nothing.
    }
}

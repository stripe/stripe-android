package com.stripe.android.paymentsheet.ui

import kotlinx.coroutines.flow.StateFlow
import org.mockito.Mockito

internal object FakeAddPaymentMethodInteractor : AddPaymentMethodInteractor {
    override val state: StateFlow<AddPaymentMethodInteractor.State>
        get() = Mockito.mock()

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        // Do nothing.
    }

    override fun close() {
        // Do nothing.
    }
}

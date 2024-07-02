package com.stripe.android.paymentsheet.ui

import kotlinx.coroutines.flow.StateFlow

internal object FakeAddPaymentMethodInteractor : AddPaymentMethodInteractor {
    override val state: StateFlow<AddPaymentMethodInteractor.State>
        get() = throw NotImplementedError(
            "State has not been implemented for FakeAddPaymentMethodInteractor"
        )

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        // Do nothing.
    }

    override fun close() {
        // Do nothing.
    }
}

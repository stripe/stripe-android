package com.stripe.android.paymentsheet.navigation

import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import kotlinx.coroutines.flow.StateFlow

internal class FakeAddPaymentMethodInteractor(
    override val state: StateFlow<AddPaymentMethodInteractor.State>,
) : AddPaymentMethodInteractor {
    override val isLiveMode: Boolean = true

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        // Do nothing.
    }

    override fun close() {
        // Do nothing.
    }
}

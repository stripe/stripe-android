package com.stripe.android.paymentelement.embedded.manage

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakeEmbeddedSavedPaymentMethodSelector(
    var result: Result<Unit>,
) : EmbeddedSavedPaymentMethodSelector {
    val selectCalls = Turbine<PaymentSelection.Saved>()

    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        selectCalls.add(selection)
        return result
    }

    fun ensureAllEventsConsumed() {
        selectCalls.ensureAllEventsConsumed()
    }
}

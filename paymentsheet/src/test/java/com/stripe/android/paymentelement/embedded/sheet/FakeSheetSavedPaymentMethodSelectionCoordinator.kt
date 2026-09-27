package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakeSheetSavedPaymentMethodSelectionCoordinator(
    var result: Result<Unit>,
) : SheetSavedPaymentMethodSelectionCoordinator {
    val selectCalls = Turbine<PaymentSelection.Saved>()

    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        selectCalls.add(selection)
        return result
    }

    fun validate() {
        selectCalls.ensureAllEventsConsumed()
    }
}

package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine

internal class FakeSheetActivityContinueCoordinator : SheetActivityContinueCoordinator {
    val onContinueCalls = Turbine<Unit>()

    override fun onContinue() {
        onContinueCalls.add(Unit)
    }

    fun validate() {
        onContinueCalls.ensureAllEventsConsumed()
    }
}

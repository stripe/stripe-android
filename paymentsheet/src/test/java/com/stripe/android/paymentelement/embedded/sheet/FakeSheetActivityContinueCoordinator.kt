package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine

internal class FakeSheetActivityContinueCoordinator : SheetActivityContinueCoordinator {
    val onContinueCalls = Turbine<Unit>()
    val completeSelectionCalls = Turbine<Unit>()

    override fun onContinue() {
        onContinueCalls.add(Unit)
    }

    override fun completeSelection() {
        completeSelectionCalls.add(Unit)
    }

    fun validate() {
        onContinueCalls.ensureAllEventsConsumed()
        completeSelectionCalls.ensureAllEventsConsumed()
    }
}

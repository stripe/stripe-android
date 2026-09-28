package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine

internal class FakeSheetActivityConfirmationHelper : SheetActivityConfirmationHelper {
    val confirmCalls = Turbine<Unit>()

    override fun confirm() {
        confirmCalls.add(Unit)
    }

    fun validate() {
        confirmCalls.ensureAllEventsConsumed()
    }
}

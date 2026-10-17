package com.stripe.android.elements.ece

import app.cash.turbine.Turbine

internal class FakeExpressCheckoutElementConfirmationPerformer : ExpressCheckoutElementConfirmationPerformer {
    val calls = Turbine<ExpressButton>()

    override fun confirm(expressButton: ExpressButton) {
        calls.add(expressButton)
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }
}

package com.stripe.android.elements.ece

import app.cash.turbine.Turbine
internal class FakeExpressCheckoutElementEventReporter : ExpressCheckoutElementEventReporter {
    val calls = Turbine<Call>()

    override fun onEceDisplayed() {
        calls.add(Call.OnEceDisplayed)
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    sealed interface Call {
        data object OnEceDisplayed : Call
    }
}

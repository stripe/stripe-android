package com.stripe.android.elements.ece

import app.cash.turbine.Turbine
internal class FakeExpressCheckoutElementEventReporter : ExpressCheckoutElementEventReporter {
    val calls = Turbine<Call>()

    override fun onEceDisplayed(content: ExpressCheckoutElementIntegration.Content?) {
        calls.add(Call.OnEceDisplayed(content))
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    sealed interface Call {
        data class OnEceDisplayed(val content: ExpressCheckoutElementIntegration.Content?) : Call
    }
}

package com.stripe.android.checkout.ece

import app.cash.turbine.Turbine

internal class FakeExpressCheckoutElementEventReporter : ExpressCheckoutElementEventReporter {
    val calls = Turbine<Call>()

    override fun onEceDisplayed() {
        calls.add(Call.OnEceDisplayed)
    }

    override fun onEceWalletTapped(expressButton: ExpressButton) {
        calls.add(Call.OnEceWalletTapped(expressButton))
    }
    
    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    sealed interface Call {
        data object OnEceDisplayed : Call

        data class OnEceWalletTapped(val expressButton: ExpressButton) : Call
    }
}

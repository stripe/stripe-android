package com.stripe.android.checkout.ece

import app.cash.turbine.Turbine

internal class FakeExpressCheckoutElementEventReporter : ExpressCheckoutElementEventReporter {
    val calls = Turbine<Call>()

    override fun onEceDisplayed() {
        calls.add(Call.OnEceDisplayed)
    }

    override fun onEceWalletTapped() {
        calls.add(Call.OnEceWalletTapped)
    }

    override fun onEcePaymentSuccess() {
        calls.add(Call.OnEcePaymentSuccess)
    }

    override fun onEcePaymentFailure() {
        calls.add(Call.OnEcePaymentFailure)
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    enum class Call {
        OnEceDisplayed,
        OnEceWalletTapped,
        OnEcePaymentSuccess,
        OnEcePaymentFailure,
    }
}

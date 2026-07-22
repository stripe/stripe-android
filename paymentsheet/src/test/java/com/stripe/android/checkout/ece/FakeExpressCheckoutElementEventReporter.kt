package com.stripe.android.checkout.ece

internal class FakeExpressCheckoutElementEventReporter : ExpressCheckoutElementEventReporter {
    val calls = mutableListOf<Call>()

    override fun onEceDisplayed() {
        calls.add(Call.OnEceDisplayed)
    }

    override fun onEceWalletTapped() {
        calls.add(Call.OnEceWalletTapped)
    }

    enum class Call {
        OnEceDisplayed,
        OnEceWalletTapped,
    }
}

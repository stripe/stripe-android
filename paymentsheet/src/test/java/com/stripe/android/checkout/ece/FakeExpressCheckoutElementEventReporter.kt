package com.stripe.android.checkout.ece

internal class FakeExpressCheckoutElementEventReporter : ExpressCheckoutElementEventReporter {
    val calls = mutableListOf<Call>()

    override fun onEceDisplayed() {
        calls.add(Call.OnEceDisplayed)
    }

    override fun onEceWalletTapped(expressButton: ExpressButton) {
        calls.add(Call.OnEceWalletTapped(expressButton))
    }

    sealed interface Call {
        data object OnEceDisplayed : Call

        data class OnEceWalletTapped(val expressButton: ExpressButton) : Call
    }
}

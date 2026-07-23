package com.stripe.android.checkout.ece

internal class FakeExpressCheckoutElementConfirmationPerformer : ExpressCheckoutElementConfirmationPerformer {
    val calls = mutableListOf<ExpressButton>()

    override fun confirm(expressButton: ExpressButton) {
        calls.add(expressButton)
    }
}

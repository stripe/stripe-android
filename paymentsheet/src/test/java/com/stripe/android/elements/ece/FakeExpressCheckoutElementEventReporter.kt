package com.stripe.android.elements.ece

import app.cash.turbine.Turbine
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler

internal class FakeExpressCheckoutElementEventReporter : ExpressCheckoutElementEventReporter {
    val calls = Turbine<Call>()

    override fun onEceDisplayed() {
        calls.add(Call.OnEceDisplayed)
    }

    override fun onEceWalletTapped(expressButton: ExpressButton) {
        calls.add(Call.OnEceWalletTapped(expressButton))
    }

    override fun onEcePaymentSuccess(expressButton: ExpressButton) {
        calls.add(Call.OnEcePaymentSuccess(expressButton))
    }

    override fun onEcePaymentFailure(
        expressButton: ExpressButton,
        error: ConfirmationHandler.Result.Failed,
    ) {
        calls.add(Call.OnEcePaymentFailure(expressButton, error))
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    sealed interface Call {
        data object OnEceDisplayed : Call

        data class OnEceWalletTapped(val expressButton: ExpressButton) : Call

        data class OnEcePaymentSuccess(val expressButton: ExpressButton) : Call

        data class OnEcePaymentFailure(
            val expressButton: ExpressButton,
            val error: ConfirmationHandler.Result.Failed,
        ) : Call
    }
}

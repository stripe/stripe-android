package com.stripe.android.testing

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher

class FakePaymentLauncher : PaymentLauncher {
    private val _calls = Turbine<Call>()
    val calls: ReceiveTurbine<Call> = _calls

    override fun confirm(params: ConfirmPaymentIntentParams) {
        _calls.add(Call.Confirm.PaymentIntent(params))
    }

    override fun confirm(params: ConfirmSetupIntentParams) {
        _calls.add(Call.Confirm.SetupIntent(params))
    }

    override fun handleNextActionForPaymentIntent(clientSecret: String) {
        _calls.add(Call.HandleNextAction.PaymentIntent(clientSecret))
    }

    override fun handleNextActionForSetupIntent(clientSecret: String) {
        _calls.add(Call.HandleNextAction.SetupIntent(clientSecret))
    }

    sealed interface Call {
        sealed interface Confirm<T : ConfirmStripeIntentParams> : Call {
            val params: T

            data class PaymentIntent(
                override val params: ConfirmPaymentIntentParams
            ) : Confirm<ConfirmPaymentIntentParams>

            data class SetupIntent(
                override val params: ConfirmSetupIntentParams
            ) : Confirm<ConfirmSetupIntentParams>
        }

        sealed interface HandleNextAction : Call {
            val clientSecret: String

            data class PaymentIntent(
                override val clientSecret: String
            ) : HandleNextAction

            data class SetupIntent(
                override val clientSecret: String
            ) : HandleNextAction
        }
    }
}

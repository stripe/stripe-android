package com.stripe.android.payments

import android.content.Context
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

internal class PaymentFlowFailureMessageFactory(
    private val context: Context
) {
    fun create(
        intent: StripeIntent,
        @StripeIntentResult.Outcome outcome: Int
    ) = when {
        intent.status == StripeIntent.Status.RequiresPaymentMethod -> {
            when (intent) {
                is PaymentIntent -> {
                    createForPaymentIntent(intent)
                }
                is SetupIntent -> {
                    createForSetupIntent(intent)
                }
                else -> null
            }
        }
        outcome == StripeIntentResult.Outcome.TIMEDOUT -> {
            context.resources.getString(R.string.stripe_failure_reason_timed_out)
        }
        else -> {
            null
        }
    }

    private fun createForPaymentIntent(
        paymentIntent: PaymentIntent
    ) = when {
        paymentIntent.lastPaymentError?.code == PaymentIntent.Error.CODE_AUTHENTICATION_ERROR -> {
            context.resources.getString(R.string.stripe_failure_reason_authentication)
        }
        paymentIntent.lastPaymentError?.type == PaymentIntent.Error.Type.CardError -> {
            paymentIntent.lastPaymentError.message
        }
        else -> {
            null
        }
    }

    private fun createForSetupIntent(
        setupIntent: SetupIntent
    ) = when {
        setupIntent.lastSetupError?.code == SetupIntent.Error.CODE_AUTHENTICATION_ERROR -> {
            context.resources.getString(R.string.stripe_failure_reason_authentication)
        }
        setupIntent.lastSetupError?.type == SetupIntent.Error.Type.CardError -> {
            setupIntent.lastSetupError.message
        }
        else -> {
            null
        }
    }
}

package com.stripe.android.payments

import android.content.Context
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.withLocalizedMessage

internal class PaymentFlowFailureMessageFactory(
    private val context: Context
) {
    fun create(
        intent: StripeIntent,
        @StripeIntentResult.Outcome outcome: Int
    ) = when {
        outcome == StripeIntentResult.Outcome.TIMEDOUT -> {
            context.resources.getString(R.string.stripe_failure_reason_timed_out)
        }
        intent.is3DS2() -> {
            null
        }
        (intent.status == StripeIntent.Status.RequiresPaymentMethod) ||
            (intent.status == StripeIntent.Status.RequiresAction) -> {
            when (intent) {
                is PaymentIntent -> {
                    createForPaymentIntent(intent)
                }
                is SetupIntent -> {
                    createForSetupIntent(intent)
                }
            }
        }
        else -> {
            null
        }
    }

    private fun createForPaymentIntent(
        paymentIntent: PaymentIntent
    ) = when {
        (
            paymentIntent.status == StripeIntent.Status.RequiresAction &&
                paymentIntent.paymentMethod?.type?.isVoucher != true
            ) ||
            (
                paymentIntent.lastPaymentError?.code ==
                    PaymentIntent.Error.CODE_AUTHENTICATION_ERROR
                ) -> {
            context.resources.getString(R.string.stripe_failure_reason_authentication)
        }
        paymentIntent.lastPaymentError?.type == PaymentIntent.Error.Type.CardError -> {
            paymentIntent.lastPaymentError.withLocalizedMessage(context).message
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
            setupIntent.lastSetupError.withLocalizedMessage(context).message
        }
        else -> {
            null
        }
    }

    private fun StripeIntent.is3DS2(): Boolean {
        return paymentMethod?.type == PaymentMethod.Type.Card &&
            nextActionData is StripeIntent.NextActionData.SdkData.Use3DS2
    }
}

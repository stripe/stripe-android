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
        requestId: String?,
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
                    createForPaymentIntent(intent, requestId)
                }
                is SetupIntent -> {
                    createForSetupIntent(intent, requestId)
                }
            }
        }
        else -> {
            null
        }
    }

    private fun createForPaymentIntent(
        paymentIntent: PaymentIntent,
        requestId: String?,
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
        else -> {
            paymentIntent.lastPaymentError?.withLocalizedMessage(
                context = context,
                requestId = requestId,
                isLiveMode = paymentIntent.isLiveMode,
            )?.message
        }
    }

    private fun createForSetupIntent(
        setupIntent: SetupIntent,
        requestId: String?,
    ) = when {
        setupIntent.lastSetupError?.code == SetupIntent.Error.CODE_AUTHENTICATION_ERROR -> {
            context.resources.getString(R.string.stripe_failure_reason_authentication)
        }
        else -> {
            setupIntent.lastSetupError?.withLocalizedMessage(
                context = context,
                requestId = requestId,
                isLiveMode = setupIntent.isLiveMode,
            )?.message
        }
    }

    private fun StripeIntent.is3DS2(): Boolean {
        return paymentMethod?.type == PaymentMethod.Type.Card &&
            nextActionData is StripeIntent.NextActionData.SdkData.Use3DS2
    }
}

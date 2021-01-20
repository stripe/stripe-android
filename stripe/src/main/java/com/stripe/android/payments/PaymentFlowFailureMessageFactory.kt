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
    ): String? {
        return when {
            intent.status == StripeIntent.Status.RequiresPaymentMethod -> {
                when (intent) {
                    is PaymentIntent -> {
                        when {
                            intent.lastPaymentError?.code == PaymentIntent.Error.CODE_AUTHENTICATION_ERROR -> {
                                context.resources.getString(R.string.stripe_failure_reason_authentication)
                            }
                            intent.lastPaymentError?.type == PaymentIntent.Error.Type.CardError -> {
                                intent.lastPaymentError.message
                            }
                            else -> {
                                null
                            }
                        }
                    }
                    is SetupIntent -> {
                        when {
                            intent.lastSetupError?.code == SetupIntent.Error.CODE_AUTHENTICATION_ERROR -> {
                                context.resources.getString(R.string.stripe_failure_reason_authentication)
                            }
                            intent.lastSetupError?.type == SetupIntent.Error.Type.CardError -> {
                                intent.lastSetupError.message
                            }
                            else -> {
                                null
                            }
                        }
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
    }
}

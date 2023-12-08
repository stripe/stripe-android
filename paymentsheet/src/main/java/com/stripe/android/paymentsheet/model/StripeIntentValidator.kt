package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntent.ConfirmationMethod.Automatic
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.Status.Canceled
import com.stripe.android.model.StripeIntent.Status.RequiresCapture
import com.stripe.android.model.StripeIntent.Status.Succeeded
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException

internal fun ElementsSession.requireValidOrThrow(): ElementsSession {
    StripeIntentValidator.requireValid(stripeIntent)
    return this
}

/**
 * Validator for [PaymentIntent] or [SetupIntent] instances used in PaymentSheet.
 */
internal object StripeIntentValidator {

    fun requireValid(
        stripeIntent: StripeIntent
    ): StripeIntent {
        val paymentMethod = stripeIntent.paymentMethod

        val exception = when {
            stripeIntent is PaymentIntent && stripeIntent.confirmationMethod != Automatic -> {
                PaymentSheetLoadingException.InvalidConfirmationMethod(stripeIntent.confirmationMethod)
            }
            stripeIntent is PaymentIntent && stripeIntent.isInTerminalState -> {
                PaymentSheetLoadingException.PaymentIntentInTerminalState(paymentMethod, stripeIntent.status)
            }
            stripeIntent is PaymentIntent && (stripeIntent.amount == null || stripeIntent.currency == null) -> {
                PaymentSheetLoadingException.MissingAmountOrCurrency
            }
            stripeIntent is SetupIntent && stripeIntent.isInTerminalState -> {
                PaymentSheetLoadingException.SetupIntentInTerminalState(paymentMethod, stripeIntent.status)
            }
            else -> {
                // valid
                null
            }
        }

        if (exception != null) {
            throw exception
        }

        return stripeIntent
    }
}

private val PaymentIntent.isInTerminalState: Boolean
    get() = status in setOf(Canceled, Succeeded, RequiresCapture)

private val SetupIntent.isInTerminalState: Boolean
    get() = status in setOf(Canceled, Succeeded)

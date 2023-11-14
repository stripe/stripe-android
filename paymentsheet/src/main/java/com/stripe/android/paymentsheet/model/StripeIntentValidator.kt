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

internal fun ElementsSession.requireValidOrThrow(
    allowSuccessState: Boolean,
): ElementsSession {
    StripeIntentValidator.requireValid(stripeIntent, allowSuccessState)
    return this
}

/**
 * Validator for [PaymentIntent] or [SetupIntent] instances used in PaymentSheet.
 */
internal object StripeIntentValidator {

    fun requireValid(
        stripeIntent: StripeIntent,
        allowSuccessState: Boolean = false,
    ): StripeIntent {
        val exception = when {
            stripeIntent is PaymentIntent && stripeIntent.confirmationMethod != Automatic -> {
                PaymentSheetLoadingException.InvalidConfirmationMethod(stripeIntent.confirmationMethod)
            }
            stripeIntent is PaymentIntent && stripeIntent.isInTerminalState(allowSuccessState) -> {
                PaymentSheetLoadingException.PaymentIntentInTerminalState(stripeIntent.status)
            }
            stripeIntent is PaymentIntent && (stripeIntent.amount == null || stripeIntent.currency == null) -> {
                PaymentSheetLoadingException.MissingAmountOrCurrency
            }
            stripeIntent is SetupIntent && stripeIntent.isInTerminalState(allowSuccessState) -> {
                PaymentSheetLoadingException.SetupIntentInTerminalState(stripeIntent.status)
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

private fun PaymentIntent.isInTerminalState(allowSuccessState: Boolean): Boolean {
    val terminalStates = if (allowSuccessState) {
        setOf(Canceled, RequiresCapture)
    } else {
        setOf(Canceled, Succeeded, RequiresCapture)
    }

    return status in terminalStates
}

private fun SetupIntent.isInTerminalState(allowSuccessState: Boolean): Boolean {
    val terminalStates = if (allowSuccessState) {
        setOf(Canceled)
    } else {
        setOf(Canceled, Succeeded)
    }

    return status in terminalStates
}

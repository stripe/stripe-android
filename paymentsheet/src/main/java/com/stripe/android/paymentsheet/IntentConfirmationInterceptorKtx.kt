package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent

internal suspend fun IntentConfirmationInterceptor.intercept(
    initializationMode: PaymentSheet.InitializationMode,
    confirmationOption: PaymentConfirmationOption<*>?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?,
    context: Context,
): IntentConfirmationInterceptor.NextStep {
    return when (confirmationOption) {
        is PaymentConfirmationOption.PaymentMethod.New -> {
            val arguments = confirmationOption.arguments

            intercept(
                initializationMode = initializationMode,
                paymentMethodOptionsParams = arguments.optionsParams,
                paymentMethodCreateParams = arguments.createParams,
                shippingValues = shippingValues,
                customerRequestedSave = arguments.shouldSave,
            )
        }
        is PaymentConfirmationOption.PaymentMethod.Saved -> {
            val arguments = confirmationOption.arguments

            intercept(
                initializationMode = initializationMode,
                paymentMethod = arguments.paymentMethod,
                paymentMethodOptionsParams = arguments.optionsParams,
                shippingValues = shippingValues,
            )
        }
        null -> {
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = IllegalStateException("Nothing selected."),
                message = resolvableString(R.string.stripe_something_went_wrong),
            )
        }
        else -> {
            val exception = IllegalStateException(
                "Attempting to confirm intent for invalid confirmation option: $confirmationOption"
            )
            val errorReporter = ErrorReporter.createFallbackInstance(context)
            errorReporter.report(
                errorEvent = UnexpectedErrorEvent.INTENT_CONFIRMATION_INTERCEPTOR_INVALID_PAYMENT_SELECTION,
                stripeException = StripeException.create(exception),
            )
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = exception,
                message = resolvableString(R.string.stripe_something_went_wrong),
            )
        }
    }
}

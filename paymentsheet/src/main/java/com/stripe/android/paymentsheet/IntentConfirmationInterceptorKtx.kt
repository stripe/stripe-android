package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent

internal suspend fun IntentConfirmationInterceptor.intercept(
    initializationMode: PaymentSheet.InitializationMode,
    confirmationOption: PaymentConfirmationOption?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?,
    context: Context,
): IntentConfirmationInterceptor.NextStep {
    return when (confirmationOption) {
        is PaymentConfirmationOption.New -> {
            intercept(
                initializationMode = initializationMode,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                paymentMethodCreateParams = confirmationOption.createParams,
                shippingValues = shippingValues,
                customerRequestedSave = confirmationOption.shouldSave,
            )
        }
        is PaymentConfirmationOption.Saved -> {
            intercept(
                initializationMode = initializationMode,
                paymentMethod = confirmationOption.paymentMethod,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
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

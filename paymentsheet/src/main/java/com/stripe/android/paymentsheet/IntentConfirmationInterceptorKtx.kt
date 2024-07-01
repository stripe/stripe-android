package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave

internal suspend fun IntentConfirmationInterceptor.intercept(
    initializationMode: PaymentSheet.InitializationMode,
    paymentSelection: PaymentSelection?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?,
    context: Context,
): IntentConfirmationInterceptor.NextStep {
    return when (paymentSelection) {
        is PaymentSelection.New -> {
            intercept(
                initializationMode = initializationMode,
                paymentMethodOptionsParams = paymentSelection.paymentMethodOptionsParams,
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                shippingValues = shippingValues,
                customerRequestedSave = paymentSelection.customerRequestedSave == CustomerRequestedSave.RequestReuse,
            )
        }
        is PaymentSelection.Saved -> {
            intercept(
                initializationMode = initializationMode,
                paymentMethod = paymentSelection.paymentMethod,
                paymentMethodOptionsParams = paymentSelection.paymentMethodOptionsParams,
                shippingValues = shippingValues,
            )
        }
        null -> {
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = IllegalStateException("Nothing selected."),
                message = context.getString(R.string.stripe_something_went_wrong),
            )
        }
        else -> {
            val exception =
                IllegalStateException("Attempting to confirm intent for invalid payment selection: $paymentSelection")
            val errorReporter = ErrorReporter.createFallbackInstance(context)
            errorReporter.report(
                errorEvent = UnexpectedErrorEvent.INTENT_CONFIRMATION_INTERCEPTOR_INVALID_PAYMENT_SELECTION,
                stripeException = StripeException.create(exception),
            )
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = exception,
                message = context.getString(R.string.stripe_something_went_wrong),
            )
        }
    }
}

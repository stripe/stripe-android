package com.stripe.android.paymentsheet

import com.stripe.android.IntentConfirmationInterceptor
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmPaymentIntentParams.SetupFutureUsage
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave

internal suspend fun IntentConfirmationInterceptor.intercept(
    clientSecret: String?,
    paymentSelection: PaymentSelection?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?,
): IntentConfirmationInterceptor.NextStep {
    return when (paymentSelection) {
        is PaymentSelection.New -> {
            val setupFutureUsage = when (paymentSelection.customerRequestedSave) {
                CustomerRequestedSave.RequestReuse -> SetupFutureUsage.OffSession
                CustomerRequestedSave.RequestNoReuse -> SetupFutureUsage.Blank
                CustomerRequestedSave.NoRequest -> null
            }

            intercept(
                clientSecret = clientSecret,
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                shippingValues = shippingValues,
                setupForFutureUsage = setupFutureUsage,
            )
        }
        is PaymentSelection.Saved -> {
            intercept(
                clientSecret = clientSecret,
                paymentMethod = paymentSelection.paymentMethod,
                shippingValues = shippingValues,
                setupForFutureUsage = null,
            )
        }
        else -> {
            error("Attempting to confirm intent for invalid payment selection: $paymentSelection")
        }
    }
}

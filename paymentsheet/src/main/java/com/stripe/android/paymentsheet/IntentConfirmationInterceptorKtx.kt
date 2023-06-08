package com.stripe.android.paymentsheet

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmPaymentIntentParams.SetupFutureUsage
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave

internal suspend fun IntentConfirmationInterceptor.intercept(
    initializationMode: PaymentSheet.InitializationMode,
    paymentSelection: PaymentSelection?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?,
    isFlowController: Boolean,
): IntentConfirmationInterceptor.NextStep {
    return when (paymentSelection) {
        is PaymentSelection.New -> {
            val setupFutureUsage = when (paymentSelection.customerRequestedSave) {
                CustomerRequestedSave.RequestReuse -> SetupFutureUsage.OffSession
                CustomerRequestedSave.RequestNoReuse -> SetupFutureUsage.Blank
                CustomerRequestedSave.NoRequest -> null
            }

            intercept(
                initializationMode = initializationMode,
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                shippingValues = shippingValues,
                setupForFutureUsage = setupFutureUsage,
                isFlowController = isFlowController,
            )
        }
        is PaymentSelection.Saved -> {
            intercept(
                initializationMode = initializationMode,
                paymentMethod = paymentSelection.paymentMethod,
                shippingValues = shippingValues,
                setupForFutureUsage = null,
                isFlowController = isFlowController,
            )
        }
        else -> {
            error("Attempting to confirm intent for invalid payment selection: $paymentSelection")
        }
    }
}

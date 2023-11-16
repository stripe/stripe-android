package com.stripe.android.paymentsheet

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave

internal suspend fun IntentConfirmationInterceptor.intercept(
    initializationMode: PaymentSheet.InitializationMode,
    paymentSelection: PaymentSelection?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?,
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
                shippingValues = shippingValues,
                customerRequestedSave = false,
            )
        }
        else -> {
            error("Attempting to confirm intent for invalid payment selection: $paymentSelection")
        }
    }
}

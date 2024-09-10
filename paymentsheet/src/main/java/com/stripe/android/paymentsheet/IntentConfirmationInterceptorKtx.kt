package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping

internal suspend fun IntentConfirmationInterceptor.intercept(
    confirmationOption: PaymentConfirmationOption.PaymentMethod,
): IntentConfirmationInterceptor.NextStep {
    return when (confirmationOption) {
        is PaymentConfirmationOption.PaymentMethod.New -> {
            intercept(
                initializationMode = confirmationOption.initializationMode,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                paymentMethodCreateParams = confirmationOption.createParams,
                shippingValues = confirmationOption.shippingDetails?.toConfirmPaymentIntentShipping(),
                customerRequestedSave = confirmationOption.shouldSave,
            )
        }
        is PaymentConfirmationOption.PaymentMethod.Saved -> {
            intercept(
                initializationMode = confirmationOption.initializationMode,
                paymentMethod = confirmationOption.paymentMethod,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                shippingValues = confirmationOption.shippingDetails?.toConfirmPaymentIntentShipping(),
            )
        }
    }
}

package com.stripe.android.paymentelement.confirmation

import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping

internal suspend fun IntentConfirmationInterceptor.intercept(
    confirmationOption: PaymentMethodConfirmationOption,
): IntentConfirmationInterceptor.NextStep {
    return when (confirmationOption) {
        is PaymentMethodConfirmationOption.New -> {
            intercept(
                initializationMode = confirmationOption.initializationMode,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                paymentMethodCreateParams = confirmationOption.createParams,
                shippingValues = confirmationOption.shippingDetails?.toConfirmPaymentIntentShipping(),
                customerRequestedSave = confirmationOption.shouldSave,
            )
        }
        is PaymentMethodConfirmationOption.Saved -> {
            intercept(
                initializationMode = confirmationOption.initializationMode,
                paymentMethod = confirmationOption.paymentMethod,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                shippingValues = confirmationOption.shippingDetails?.toConfirmPaymentIntentShipping(),
            )
        }
    }
}

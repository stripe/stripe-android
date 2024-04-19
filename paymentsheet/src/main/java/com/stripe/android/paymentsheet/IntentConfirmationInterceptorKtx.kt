package com.stripe.android.paymentsheet

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave
import java.lang.IllegalStateException

internal suspend fun IntentConfirmationInterceptor.intercept(
    initializationMode: PaymentSheet.InitializationMode,
    paymentSelection: PaymentSelection?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?,
): IntentConfirmationInterceptor.NextStep {
    return when (paymentSelection) {
        is PaymentSelection.New -> {
            if (paymentSelection.isExternalPaymentMethod()) {
                return if (ExternalPaymentMethodInterceptor.externalPaymentMethodCreator == null) {
                    IntentConfirmationInterceptor.NextStep.Fail(
                        cause = IllegalStateException("null EPM creator"),
                        message = "null EPM creator"
                    )
                } else {
                    IntentConfirmationInterceptor.NextStep.HandleExternalPaymentMethod(
                        paymentMethodCode = paymentSelection.paymentMethodCreateParams.typeCode,
                        billingDetails = PaymentSheet.BillingDetails() // TODO: can get from payment selection but is wrong type
                    )
                }
            }
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
                requiresSaveOnConfirmation = paymentSelection.requiresSaveOnConfirmation,
            )
        }
        else -> {
            error("Attempting to confirm intent for invalid payment selection: $paymentSelection")
        }
    }
}

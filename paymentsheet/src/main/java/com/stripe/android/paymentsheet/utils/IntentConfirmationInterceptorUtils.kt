package com.stripe.android.paymentsheet.utils

import com.stripe.android.interceptor.IntentConfirmationInterceptor
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.paymentsheet.model.PaymentSelection

internal suspend fun IntentConfirmationInterceptor.intercept(
    clientSecret: String?,
    paymentSelection: PaymentSelection?,
    shippingValues: ConfirmPaymentIntentParams.Shipping?
): IntentConfirmationInterceptor.NextStep {
    return when (paymentSelection) {
        PaymentSelection.GooglePay -> {
            return IntentConfirmationInterceptor.NextStep.Fail("GooglePay is not available in the DeferredFlow")
        }
        PaymentSelection.Link -> {
            return IntentConfirmationInterceptor.NextStep.Fail("Link is not available in the DeferredFlow")
        }
        is PaymentSelection.New -> {
            intercept(
                clientSecret = clientSecret,
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                shippingValues = shippingValues,
                setupForFutureUsage = when (paymentSelection.customerRequestedSave) {
                    PaymentSelection.CustomerRequestedSave.RequestReuse -> {
                        ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    }
                    PaymentSelection.CustomerRequestedSave.RequestNoReuse -> {
                        ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                    }
                    PaymentSelection.CustomerRequestedSave.NoRequest -> {
                        null
                    }
                }
            )
        }
        is PaymentSelection.Saved -> {
            intercept(
                clientSecret = clientSecret,
                paymentMethod = paymentSelection.paymentMethod,
                shippingValues = shippingValues,
                setupForFutureUsage = null
            )
        }
        null -> {
            return IntentConfirmationInterceptor.NextStep.Fail("Something went wrong in the DeferredFlow")
        }
    }
}
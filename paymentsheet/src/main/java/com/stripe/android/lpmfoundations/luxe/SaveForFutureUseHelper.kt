package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent

internal fun isSaveForFutureUseValueChangeable(
    code: PaymentMethodCode,
    metadata: PaymentMethodMetadata,
): Boolean {
    return when (metadata.stripeIntent) {
        is PaymentIntent -> {
            val isSetupFutureUsageSet = metadata.stripeIntent.isSetupFutureUsageSet(code)

            if (isSetupFutureUsageSet) {
                false
            } else {
                metadata.hasCustomerConfiguration
            }
        }

        is SetupIntent -> {
            false
        }
    }
}

package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

internal fun isSaveForFutureUseValueChangeable(
    code: PaymentMethodCode,
    paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
    intent: StripeIntent,
    hasCustomerConfiguration: Boolean,
): Boolean {
    return when (paymentMethodSaveConsentBehavior) {
        is PaymentMethodSaveConsentBehavior.Disabled -> false
        is PaymentMethodSaveConsentBehavior.Enabled -> hasCustomerConfiguration
        is PaymentMethodSaveConsentBehavior.Legacy -> {
            when (intent) {
                is PaymentIntent -> {
                    val isSetupFutureUsageSet = intent.isSetupFutureUsageSet(code)

                    if (isSetupFutureUsageSet) {
                        false
                    } else {
                        hasCustomerConfiguration
                    }
                }

                is SetupIntent -> {
                    false
                }
            }
        }
    }
}

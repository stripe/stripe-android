package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.ElementsSession

internal fun ElementsSession.toPaymentSheetSaveConsentBehavior(): PaymentMethodSaveConsentBehavior {
    return when (val paymentSheetComponent = customer?.session?.components?.paymentSheet) {
        is ElementsSession.Customer.Components.PaymentSheet.Enabled -> {
            if (paymentSheetComponent.isPaymentMethodSaveEnabled) {
                PaymentMethodSaveConsentBehavior.Enabled
            } else {
                PaymentMethodSaveConsentBehavior.Disabled(
                    overrideAllowRedisplay = paymentSheetComponent.allowRedisplayOverride
                )
            }
        }
        // Unless the merchant explicitly defines the consent behavior, always use the legacy behavior
        is ElementsSession.Customer.Components.PaymentSheet.Disabled,
        null -> PaymentMethodSaveConsentBehavior.Legacy
    }
}

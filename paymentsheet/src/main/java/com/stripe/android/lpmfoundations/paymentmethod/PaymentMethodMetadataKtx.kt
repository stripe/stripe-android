package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod

internal fun ElementsSession.toPaymentSheetSaveConsentBehavior(): PaymentMethodSaveConsentBehavior {
    return when (val mobilePaymentElementComponent = customer?.session?.components?.mobilePaymentElement) {
        is ElementsSession.Customer.Components.MobilePaymentElement.Enabled -> {
            if (mobilePaymentElementComponent.isPaymentMethodSaveEnabled) {
                PaymentMethodSaveConsentBehavior.Enabled
            } else {
                PaymentMethodSaveConsentBehavior.Disabled(
                    overrideAllowRedisplay = mobilePaymentElementComponent.allowRedisplayOverride
                )
            }
        }
        // Unless the merchant explicitly defines the consent behavior, always use the legacy behavior
        is ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
        null -> PaymentMethodSaveConsentBehavior.Legacy
    }
}

internal fun ElementsSession.toCustomerSheetSaveConsentBehavior(): PaymentMethodSaveConsentBehavior {
    return when (customer?.session?.components?.customerSheet) {
        is ElementsSession.Customer.Components.CustomerSheet.Enabled -> {
            PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS
            )
        }
        // Unless the merchant explicitly defines the consent behavior, always use the legacy behavior
        is ElementsSession.Customer.Components.CustomerSheet.Disabled,
        null -> PaymentMethodSaveConsentBehavior.Legacy
    }
}

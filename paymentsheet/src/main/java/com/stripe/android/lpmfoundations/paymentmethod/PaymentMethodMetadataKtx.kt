package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.ElementsSession

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

internal fun ElementsSession.toDisplayableCustomPaymentMethods(
    configuration: CommonConfiguration,
): List<DisplayableCustomPaymentMethod> {
    return customPaymentMethods.mapNotNull { customPaymentMethod ->
        when (customPaymentMethod) {
            is ElementsSession.CustomPaymentMethod.Available -> {
                val associatedDefinition = configuration.customPaymentMethods.first { definedCustomPaymentMethod ->
                    customPaymentMethod.type == definedCustomPaymentMethod.id
                }

                DisplayableCustomPaymentMethod(
                    id = customPaymentMethod.type,
                    displayName = customPaymentMethod.displayName,
                    logoUrl = customPaymentMethod.logoUrl,
                    subtitle = associatedDefinition.subtitle,
                    doesNotCollectBillingDetails = associatedDefinition.disableBillingDetailCollection,
                )
            }
            is ElementsSession.CustomPaymentMethod.Unavailable -> null
        }
    }
}

package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.luxe.SetupFutureUsageFieldConfiguration
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.SharedDataSpec

internal interface PaymentMethodDefinition {
    /**
     * The payment method type, for example: PaymentMethod.Type.Card, etc.
     */
    val type: PaymentMethod.Type

    val supportedAsSavedPaymentMethod: Boolean

    /**
     * The requirements that need to be met in order for this Payment Method to be available for selection by the buyer.
     * For example emptySet() if no requirements exist.
     * Or setOf(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods) if the payment method requires the
     * merchant to provide a PaymentSheet.Configuration with delayed payment methods enabled.
     */
    fun requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement>

    fun supportedPaymentMethod(metadata: PaymentMethodMetadata, sharedDataSpec: SharedDataSpec): SupportedPaymentMethod
}

internal fun PaymentMethodDefinition.isSupported(metadata: PaymentMethodMetadata): Boolean {
    if (type.code !in metadata.stripeIntent.paymentMethodTypes) {
        return false
    }
    return requirementsToBeUsedAsNewPaymentMethod(metadata.hasIntentToSetup()).all { requirement ->
        requirement.isMetBy(metadata)
    }
}

internal fun PaymentMethodDefinition.getSetupFutureUsageFieldConfiguration(
    metadata: PaymentMethodMetadata,
    customerConfiguration: PaymentSheet.CustomerConfiguration?,
): SetupFutureUsageFieldConfiguration? {
    val oneTimeUse = SetupFutureUsageFieldConfiguration(
        showCheckbox = false,
        saveForFutureUseInitialValue = false
    )
    val merchantRequestedSave = SetupFutureUsageFieldConfiguration(
        showCheckbox = false,
        saveForFutureUseInitialValue = true
    )
    val userSelectableSave = SetupFutureUsageFieldConfiguration(
        showCheckbox = true,
        saveForFutureUseInitialValue = false
    )

    return when (metadata.stripeIntent) {
        is PaymentIntent -> {
            val isSetupFutureUsageSet = metadata.stripeIntent.isSetupFutureUsageSet(type.code)

            if (isSetupFutureUsageSet) {
                if (supportedAsSavedPaymentMethod) {
                    merchantRequestedSave
                } else {
                    null
                }
            } else {
                if (customerConfiguration != null) {
                    userSelectableSave
                } else {
                    oneTimeUse
                }
            }
        }

        is SetupIntent -> {
            if (supportedAsSavedPaymentMethod) {
                merchantRequestedSave
            } else {
                null
            }
        }
    }
}

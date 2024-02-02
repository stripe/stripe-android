package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
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
    fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement>

    fun supportedPaymentMethod(metadata: PaymentMethodMetadata, sharedDataSpec: SharedDataSpec): SupportedPaymentMethod
}

internal fun PaymentMethodDefinition.isSupported(metadata: PaymentMethodMetadata): Boolean {
    val requirements = addRequirement(metadata.hasIntentToSetup())
    for (requirement in requirements) {
        if (!requirement.meetsRequirements(metadata)) {
            return false
        }
    }
    return true
}

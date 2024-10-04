package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.PaymentMethod

internal interface PaymentMethodDefinition {
    /**
     * The payment method type, for example: PaymentMethod.Type.Card, etc.
     */
    val type: PaymentMethod.Type

    val incentiveType: String?
        get() = null

    val supportedAsSavedPaymentMethod: Boolean

    fun requiresMandate(metadata: PaymentMethodMetadata): Boolean

    /**
     * The requirements that need to be met in order for this Payment Method to be available for selection by the buyer.
     * For example emptySet() if no requirements exist.
     * Or setOf(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods) if the payment method requires the
     * merchant to provide a PaymentSheet.Configuration with delayed payment methods enabled.
     */
    fun requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement>

    fun uiDefinitionFactory(): UiDefinitionFactory
}

internal fun PaymentMethodDefinition.isSupported(metadata: PaymentMethodMetadata): Boolean {
    return requirementsToBeUsedAsNewPaymentMethod(metadata.hasIntentToSetup()).all { requirement ->
        requirement.isMetBy(metadata)
    }
}

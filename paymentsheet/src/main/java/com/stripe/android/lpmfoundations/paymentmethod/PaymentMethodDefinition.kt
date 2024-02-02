package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.elements.SharedDataSpec

internal interface PaymentMethodDefinition {
    /**
     * The payment method type, for example: PaymentMethod.Type.Card, etc.
     */
    val type: PaymentMethod.Type

    /**
     * The requirements that need to be met in order for this Payment Method to be available for selection by the buyer.
     * For example emptySet() if no requirements exist.
     * Or setOf(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods) if the payment method requires the
     * merchant to provide a PaymentSheet.Configuration with delayed payment methods enabled.
     */
    // TODO(jaynewstrom-stripe): Will be added back in a follow up.
    // fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement>

    fun supportedPaymentMethod(metadata: PaymentMethodMetadata, sharedDataSpec: SharedDataSpec): SupportedPaymentMethod?
}

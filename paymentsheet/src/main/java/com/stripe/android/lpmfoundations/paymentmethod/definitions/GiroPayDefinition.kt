package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object GiroPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Giropay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = GiroPayUiDefinitionFactory
}

private object GiroPayUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createSupportedPaymentMethod(sharedDataSpec: SharedDataSpec) = SupportedPaymentMethod(
        paymentMethodDefinition = GiroPayDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_giropay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_giropay,
    )
}

package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodIncentive
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormElement

internal object SwishDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Swish

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = SwishUiDefinitionFactory
}

private object SwishUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(
        incentive: PaymentMethodIncentive?,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = SwishDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_swish,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_swish,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments
    ): List<FormElement> {
        return FormElementsBuilder(arguments).build()
    }
}

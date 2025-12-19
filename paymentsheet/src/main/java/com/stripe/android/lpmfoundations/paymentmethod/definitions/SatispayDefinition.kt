package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement

internal object SatispayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Satispay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(): UiDefinitionFactory = SatispayUiDefinitionFactory
}

private object SatispayUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod() = SupportedPaymentMethod(
        paymentMethodDefinition = SatispayDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_satispay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_satispay_day,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_satispay_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        if (SatispayDefinition.requiresMandate(metadata)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_satispay_mandate,
                    args = listOf(arguments.merchantName)
                )
            )
        }
    }
}

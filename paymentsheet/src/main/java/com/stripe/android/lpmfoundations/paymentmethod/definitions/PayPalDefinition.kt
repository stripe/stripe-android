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

internal object PayPalDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.PayPal

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(): UiDefinitionFactory = PayPalUiDefinitionFactory
}

private object PayPalUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod() = SupportedPaymentMethod(
        paymentMethodDefinition = PayPalDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_paypal,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_paypal,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        if (PayPalDefinition.requiresMandate(metadata)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_paypal_mandate,
                    args = listOf(arguments.merchantName)
                )
            )
        }
    }
}

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
import com.stripe.android.uicore.elements.IdentifierSpec

internal object AmazonPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.AmazonPay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(): UiDefinitionFactory = AmazonPayUiDefinitionFactory
}

private object AmazonPayUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod() = SupportedPaymentMethod(
        code = PaymentMethod.Type.AmazonPay.code,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_amazon_pay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_amazon_pay,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        if (AmazonPayDefinition.requiresMandate(metadata)) {
            builder.footer(
                MandateTextElement(
                    identifier = IdentifierSpec.Generic("mandate"),
                    stringResId = R.string.stripe_amazon_pay_mandate,
                    args = listOf(arguments.merchantName)
                )
            )
        }
    }
}

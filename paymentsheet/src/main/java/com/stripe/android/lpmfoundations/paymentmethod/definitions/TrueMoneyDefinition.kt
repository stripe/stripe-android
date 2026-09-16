package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormFieldId

internal object TrueMoneyDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.TrueMoney

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = emptySet()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = metadata.hasIntentToSetup(type.code)

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = TrueMoneyUiDefinitionFactory
}

private object TrueMoneyUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = TrueMoneyDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_truemoney,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_truemoney,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder.footer(
            StaticTextElement(
                identifier = FormFieldId.Generic("truemoney_redirect"),
                stringResId = R.string.stripe_paymentsheet_redirect_instructions,
                controller = null,
            )
        )

        if (TrueMoneyDefinition.requiresMandate(metadata) && metadata.mandateAllowed(PaymentMethod.Type.TrueMoney)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_truemoney_mandate,
                    args = listOf(arguments.merchantName),
                )
            )
        }
    }
}

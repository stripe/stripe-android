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

internal object GCashDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.GCash

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = emptySet()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = metadata.hasIntentToSetup(type.code)

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = GCashUiDefinitionFactory
}

private object GCashUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = GCashDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_gcash,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_gcash,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_gcash_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder.footer(
            StaticTextElement(
                identifier = FormFieldId.Generic("gcash_redirect"),
                stringResId = R.string.stripe_paymentsheet_redirect_instructions,
                controller = null,
            )
        )

        if (GCashDefinition.requiresMandate(metadata) && metadata.mandateAllowed(PaymentMethod.Type.GCash)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_gcash_mandate,
                    args = listOf(arguments.merchantName),
                )
            )
        }
    }
}

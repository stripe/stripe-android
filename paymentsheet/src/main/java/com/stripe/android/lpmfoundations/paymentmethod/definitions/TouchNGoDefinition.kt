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

internal object TouchNGoDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.TouchNGo

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = emptySet()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = metadata.hasIntentToSetup(type.code)

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = TouchNGoUiDefinitionFactory
}

private object TouchNGoUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = TouchNGoDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_touch_n_go,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_touch_n_go,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_touch_n_go_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder.footer(
            StaticTextElement(
                identifier = FormFieldId.Generic("touch_n_go_redirect"),
                stringResId = R.string.stripe_paymentsheet_redirect_instructions,
                controller = null,
            )
        )

        if (TouchNGoDefinition.requiresMandate(metadata) && metadata.mandateAllowed(PaymentMethod.Type.TouchNGo)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_touch_n_go_mandate,
                    args = listOf(arguments.merchantName),
                )
            )
        }
    }
}

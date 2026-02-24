package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement

internal object IdealWeroDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Ideal

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOfNotNull(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods.takeIf { hasIntentToSetup },
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean =
        metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = IdealWeroUiDefinitionFactory
}

private object IdealWeroUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = IdealWeroDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_ideal,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_ideal_wero,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        builder
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .apply {
                if (metadata.hasIntentToSetup(IdealWeroDefinition.type.code)) {
                    requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
                }
            }
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .apply {
                if (IdealWeroDefinition.requiresMandate(metadata)) {
                    footer(
                        MandateTextElement(
                            stringResId = R.string.stripe_sepa_mandate,
                            args = listOf(arguments.merchantName)
                        )
                    )
                }
            }
    }
}

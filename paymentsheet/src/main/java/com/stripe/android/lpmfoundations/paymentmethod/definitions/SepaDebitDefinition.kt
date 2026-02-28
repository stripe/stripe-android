package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.addSavePaymentOptionElements
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.IbanConfig
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController

internal object SepaDebitDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.SepaDebit

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean =
        metadata.mandateAllowed(paymentMethodType = type)

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = SepaDebitUiDefinitionFactory
}

private object SepaDebitUiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val ibanIdentifier = IdentifierSpec.Generic("sepa_debit[iban]")

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        builder
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .element(
                SectionElement.wrap(
                    sectionFieldElement = SimpleTextElement(
                        identifier = ibanIdentifier,
                        controller = SimpleTextFieldController(
                            textFieldConfig = IbanConfig(),
                            initialValue = arguments.initialValues[ibanIdentifier]
                        )
                    )
                )
            )
            .requireBillingAddressIfAllowed()
            .apply {
                val elements = mutableListOf<FormElement>()

                val canSave = isSaveForFutureUseValueChangeable(
                    code = SepaDebitDefinition.type.code,
                    metadata = metadata,
                )

                if (canSave) {
                    elements.addSavePaymentOptionElements(
                        metadata = metadata,
                        arguments = arguments,
                    )
                }

                elements.forEach(::footer)

                if (SepaDebitDefinition.requiresMandate(metadata)) {
                    footer(
                        MandateTextElement(
                            stringResId = R.string.stripe_sepa_mandate,
                            args = listOf(arguments.merchantName)
                        )
                    )
                }
            }
    }

    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = SepaDebitDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_sepa_debit,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        iconResourceNight = null,
    )
}

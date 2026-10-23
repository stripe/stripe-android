package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.FormElementDefinition
import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.savePaymentOptionDefinition
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.IbanConfig
import com.stripe.android.uicore.elements.FormFieldId
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
    private val ibanIdentifier = FormFieldId.Generic("sepa_debit[iban]")

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        builder: FormElementsBuilder
    ) {
        builder
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .element(
                FormElementDefinition.interactive { arguments ->
                    SectionElement.wrap(
                        sectionFieldElement = SimpleTextElement(
                            identifier = ibanIdentifier,
                            controller = SimpleTextFieldController(
                                textFieldConfig = IbanConfig(),
                                initialValue = arguments.initialValues[ibanIdentifier]
                            )
                        )
                    )
                }
            )
            .requireBillingAddressIfAllowed()
            .apply {
                if (isSaveForFutureUseValueChangeable(SepaDebitDefinition.type.code, metadata)) {
                    footer(savePaymentOptionDefinition(metadata))
                }
            }
            .addMandateIfAllowed(
                formElement = FormElementDefinition.mandate(
                    identifier = FormFieldId.Generic("mandate"),
                    stringResId = R.string.stripe_sepa_mandate,
                    args = listOf(metadata.merchantName),
                    topPadding = 8.dp,
                )
            )
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

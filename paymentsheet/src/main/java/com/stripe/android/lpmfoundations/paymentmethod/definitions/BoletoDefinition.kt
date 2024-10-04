package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
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
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController

internal object BoletoDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Boleto

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = BoletoUiDefinitionFactory
}

private object BoletoUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(
        incentive: PaymentMethodIncentive?,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = BoletoDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_boleto,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_boleto,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments
    ): List<FormElement> {
        val taxIdElementIdentifierSpec = IdentifierSpec.Generic("boleto[tax_id]")
        val taxIdElement = SimpleTextElement(
            taxIdElementIdentifierSpec,
            SimpleTextFieldController(
                SimpleTextFieldConfig(
                    label = R.string.stripe_boleto_tax_id_label,
                    capitalization = KeyboardCapitalization.None,
                    keyboard = KeyboardType.Ascii,
                ),
                initialValue = arguments.initialValues[taxIdElementIdentifierSpec],
            )
        )
        return FormElementsBuilder(arguments)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .element(SectionElement.wrap(taxIdElement))
            .requireBillingAddressIfAllowed(setOf("BR"))
            .build()
    }
}

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
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController

internal object KonbiniDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Konbini

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = KonbiniUiDefinitionFactory
}

private object KonbiniUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(
        incentive: PaymentMethodIncentive?,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = KonbiniDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_konbini,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_konbini,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments
    ): List<FormElement> {
        val confirmationNumberElement = SimpleTextElement(
            identifier = IdentifierSpec.KonbiniConfirmationNumber,
            controller = SimpleTextFieldController(
                textFieldConfig = SimpleTextFieldConfig(
                    label = R.string.stripe_konbini_confirmation_number_label,
                    capitalization = KeyboardCapitalization.None,
                    keyboard = KeyboardType.Phone,
                ),
                initialValue = arguments.initialValues[IdentifierSpec.KonbiniConfirmationNumber],
                showOptionalLabel = true,
            ),
        )
        return FormElementsBuilder(arguments)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .element(SectionElement.wrap(confirmationNumberElement))
            .build()
    }
}

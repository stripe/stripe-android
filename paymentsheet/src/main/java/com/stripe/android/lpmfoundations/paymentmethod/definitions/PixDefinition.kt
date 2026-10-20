package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.strings.transformations.Replace
import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal object PixDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Pix

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = emptySet()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = metadata.hasIntentToSetup(type.code)

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = PixUiDefinitionFactory
}

private object PixUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = PixDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_pix,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_pix,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        if (metadata.stripeIntent.countryCode.equals("BR", ignoreCase = true)) {
            return
        }

        val taxIdFieldId = FormFieldId.Generic("billing_details[tax_id]")
        val taxIdElement = SimpleTextElement(
            identifier = taxIdFieldId,
            controller = SimpleTextFieldController(
                textFieldConfig = PixTaxIdConfig(),
                initialValue = arguments.initialValues[taxIdFieldId],
            ),
        )

        builder
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .element(SectionElement.wrap(taxIdElement))
            .footer(PixInternationalDisclosureElement())
    }
}

private class PixInternationalDisclosureElement : FormElement {
    override val identifier = FormFieldId.Generic("pix_international_disclosure")
    override val controller: InputController? = null
    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString = resolvableString(
        id = R.string.stripe_pix_international_disclosure,
        transformations = listOf(
            Replace(
                original = "<terms>",
                replacement = "<a href=\"$EBANX_TERMS_URL\">",
            ),
            Replace(original = "</terms>", replacement = "</a>"),
        ),
    )

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<FormFieldId, FormFieldEntry>>> =
        stateFlowOf(emptyList())
}

private const val EBANX_TERMS_URL =
    "https://www.ebanx.com/pt-br/legal/consumidores/brasil/termos-para-processar-pagamentos/"

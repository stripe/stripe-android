package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal object KlarnaDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Klarna

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(): UiDefinitionFactory = KlarnaUiDefinitionFactory
}

private object KlarnaUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments
    ): List<FormElement> {
        // These are the countries/regions where Klarna allows buyers
        val allowedCountryCodes: Set<String> = CountryUtils.klarnaSupportedBuyerCountries

        val formElementsBuilder = FormElementsBuilder(arguments)
            .hideCountryElement()
            .availableCountries(allowedCountryCodes)

        if (KlarnaDefinition.requiresMandate(metadata)) {
            formElementsBuilder.footer(
                getKlarnaMandateElement(
                    merchantName = metadata.merchantName
                )
            )
        }

        if (metadata.stripeIntent is SetupIntent ||
            arguments.billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full
        ) {
            formElementsBuilder.element(
                getKlarnaCountryElement(
                    allowedCountryCodes = allowedCountryCodes,
                    initialValue = metadata.stripeIntent.countryCode
                )
            )
        }

        return formElementsBuilder.build()
    }

    override fun createSupportedPaymentMethod(): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            paymentMethodDefinition = KlarnaDefinition,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            iconResourceNight = null,
            subtitle = R.string.stripe_klarna_pay_later.resolvableString
        )
    }

    private fun getKlarnaMandateElement(merchantName: String): MandateTextElement {
        return MandateTextElement(
            identifier = IdentifierSpec.Generic("klarna_mandate"),
            stringResId = R.string.stripe_klarna_mandate,
            args = listOf(merchantName, merchantName),
        )
    }

    private fun getKlarnaCountryElement(
        allowedCountryCodes: Set<String>,
        initialValue: String?
    ): FormElement {
        val identifier = IdentifierSpec.Generic("billing_details[address][country]")

        return SectionElement.wrap(
            CountryElement(
                identifier = identifier,
                controller = DropdownFieldController(
                    CountryConfig(allowedCountryCodes),
                    initialValue = initialValue
                )
            )
        )
    }
}

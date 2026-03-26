package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.StaticTextElement
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

    override fun uiDefinitionFactory(metadata: PaymentMethodMetadata): UiDefinitionFactory {
        return if (FeatureFlags.enableKlarnaFormRemoval.isEnabled) {
            KlarnaRemovedFormUiDefinitionFactory
        } else {
            KlarnaUiDefinitionFactory
        }
    }
}

private object KlarnaUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = KlarnaDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
        iconResourceNight = null,
        subtitle = R.string.stripe_klarna_pay_later.resolvableString
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder
            .header(
                formElement = StaticTextElement(
                    identifier = IdentifierSpec.Generic("klarna_header_text"),
                    stringResId = R.string.stripe_klarna_buy_now_pay_later
                )
            )
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .ignoreBillingAddressRequirements()
            .element(
                formElement = SectionElement.wrap(
                    sectionFieldElement = CountryElement(
                        identifier = IdentifierSpec.Country,
                        controller = DropdownFieldController(
                            config = CountryConfig(
                                onlyShowCountryCodes =
                                metadata.billingDetailsCollectionConfiguration.allowedBillingCountries,
                            ),
                            initialValue = arguments.initialValues[IdentifierSpec.Country],
                        )
                    )
                )
            )
            .apply {
                if (
                    metadata.billingDetailsCollectionConfiguration.address ==
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                ) {
                    AddressSpec(
                        allowedCountryCodes = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
                        hideCountry = true,
                    ).transform(
                        initialValues = arguments.initialValues,
                        shippingValues = arguments.shippingValues,
                        autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
                    ).forEach {
                        element(it)
                    }
                }

                if (KlarnaDefinition.requiresMandate(metadata)) {
                    builder.footer(
                        formElement = MandateTextElement(
                            stringResId = R.string.stripe_klarna_mandate,
                            args = listOf(arguments.merchantName, arguments.merchantName)
                        )
                    )
                }
            }
    }
}

private object KlarnaRemovedFormUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = KlarnaDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
        iconResourceNight = null,
        subtitle = R.string.stripe_klarna_pay_later.resolvableString
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        if (metadata.stripeIntent is SetupIntent &&
            arguments.billingDetailsCollectionConfiguration.address !=
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        ) {
            builder
                .header(
                    StaticTextElement(
                        IdentifierSpec.Generic("klarna_header_text"),
                        stringResId = R.string.stripe_klarna_buy_now_pay_later
                    )
                )
                .element(
                    getKlarnaCountryElement(
                        allowedCountryCodes = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
                        initialValue = metadata.stripeIntent.countryCode
                    )
                )
        }

        if (KlarnaDefinition.requiresMandate(metadata)) {
            builder.footer(
                formElement = MandateTextElement(
                    stringResId = R.string.stripe_klarna_mandate,
                    args = listOf(arguments.merchantName, arguments.merchantName)
                )
            )
        }
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

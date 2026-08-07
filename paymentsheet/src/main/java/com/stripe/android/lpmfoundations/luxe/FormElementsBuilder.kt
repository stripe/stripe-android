package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal class FormElementsBuilder(
    private val arguments: UiDefinitionFactory.Arguments,
    private val supportsAutomaticTaxBillingAddress: Boolean,
) {
    private val headerFormElements: MutableList<FormElement> = mutableListOf()
    private val formElements: MutableList<FormElement> = mutableListOf()
    private val footerFormElements: MutableList<FormElement> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> = mutableSetOf()
    private val overriddenContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()

    private var hasNormalAddressSource: Boolean = false
    private var hasCountryOnlySource: Boolean = false
    private var ignoreBillingAddressCollection: Boolean = false
    private var availableCountries: Set<String> =
        arguments.billingDetailsCollectionConfiguration.allowedBillingCountries

    init {
        // Setup the required contact information fields based on the merchant billingDetailsCollectionConfiguration.
        for (value in ContactInformationCollectionMode.entries) {
            if (value.isRequired(arguments.billingDetailsCollectionConfiguration)) {
                requireContactInformationIfAllowed(value)
            }
        }
    }

    fun header(formElement: FormElement): FormElementsBuilder = apply {
        headerFormElements += formElement
    }

    fun ignoreContactInformationRequirements() = apply {
        requiredContactInformationCollectionModes.clear()
    }

    fun requireContactInformationIfAllowed(type: ContactInformationCollectionMode): FormElementsBuilder = apply {
        if (type.isAllowed(arguments.billingDetailsCollectionConfiguration)) {
            requiredContactInformationCollectionModes += type
        }
    }

    fun overrideContactInformationPosition(type: ContactInformationCollectionMode): FormElementsBuilder = apply {
        if (type in requiredContactInformationCollectionModes) {
            overriddenContactInformationCollectionModes += type

            formElements += type.formElement(arguments.initialValues)
        }
    }

    fun element(formElement: FormElement): FormElementsBuilder = apply {
        formElements += formElement
    }

    fun countryOnly(
        allowedCountryCodes: Set<String>,
    ): FormElementsBuilder = apply {
        check(!hasCountryOnlySource) {
            "Only one country-only billing address source can be declared."
        }
        check(!hasNormalAddressSource) {
            "Country-only and normal billing address sources cannot both be declared."
        }
        hasCountryOnlySource = true

        formElements += createCountryOnlySourceFormElements(allowedCountryCodes)
    }

    fun ignoreBillingAddressRequirements() = apply {
        hasNormalAddressSource = false
        ignoreBillingAddressCollection = true
    }

    fun requireBillingAddressIfAllowed(
        availableCountries: Set<String> = this.availableCountries,
    ): FormElementsBuilder = apply {
        check(!hasCountryOnlySource) {
            "Country-only and normal billing address sources cannot both be declared."
        }
        if (arguments.billingDetailsCollectionConfiguration.address
            != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        ) {
            hasNormalAddressSource = true
            ignoreBillingAddressCollection = false

            this.availableCountries = availableCountries
        }
    }

    fun footer(formElement: FormElement): FormElementsBuilder = apply {
        footerFormElements += formElement
    }

    fun build(): List<FormElement> {
        return buildList {
            addAll(headerFormElements) // Order headers first.

            for (collectionMode in requiredContactInformationCollectionModes) {
                if (collectionMode !in overriddenContactInformationCollectionModes) {
                    add(collectionMode.formElement(arguments.initialValues))
                }
            }

            addAll(formElements)

            if (hasNormalAddressSource) {
                addAll(createFullAddressFormElements(availableCountries))
            } else if (!hasCountryOnlySource && !ignoreBillingAddressCollection) {
                addAll(createAbsentSourceFormElements())
            }

            addAll(footerFormElements) // Order footers last.
        }
    }

    private fun createCountryOnlySourceFormElements(
        allowedCountryCodes: Set<String>,
    ): List<FormElement> {
        if (ignoreBillingAddressCollection) {
            return listOf(createCountryOnlyFormElement(allowedCountryCodes))
        }

        return when (
            val result = BillingAddressSourceResolver.resolve(
                source = BillingAddressSourceResolver.Source.CountryOnly(allowedCountryCodes),
                addressCollectionMode = arguments.billingDetailsCollectionConfiguration.address,
                requiresBillingAddressForAutomaticTax = supportsAutomaticTaxBillingAddress &&
                    arguments.requiresBillingAddressForAutomaticTax,
                allowedBillingCountries = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
            )
        ) {
            is BillingAddressSourceResolver.Result.CountryOnly -> {
                listOf(createCountryOnlyFormElement(result.allowedCountryCodes))
            }
            is BillingAddressSourceResolver.Result.TaxMinimum -> {
                createTaxMinimumAddressFormElements(result.allowedCountryCodes)
            }
            is BillingAddressSourceResolver.Result.Full -> {
                createFullAddressFormElements(result.allowedCountryCodes)
            }
            BillingAddressSourceResolver.Result.PreserveExisting,
            BillingAddressSourceResolver.Result.UseExistingNormalAddress -> {
                error("A country-only billing address source must produce form elements.")
            }
        }
    }

    private fun createAbsentSourceFormElements(): List<FormElement> {
        return when (
            val result = BillingAddressSourceResolver.resolve(
                source = BillingAddressSourceResolver.Source.Absent,
                addressCollectionMode = arguments.billingDetailsCollectionConfiguration.address,
                requiresBillingAddressForAutomaticTax = supportsAutomaticTaxBillingAddress &&
                    arguments.requiresBillingAddressForAutomaticTax,
                allowedBillingCountries = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
            )
        ) {
            BillingAddressSourceResolver.Result.PreserveExisting -> emptyList()
            is BillingAddressSourceResolver.Result.TaxMinimum -> {
                createTaxMinimumAddressFormElements(result.allowedCountryCodes)
            }
            is BillingAddressSourceResolver.Result.Full -> {
                createFullAddressFormElements(result.allowedCountryCodes)
            }
            BillingAddressSourceResolver.Result.UseExistingNormalAddress,
            is BillingAddressSourceResolver.Result.CountryOnly -> {
                error("An absent billing address source cannot produce this result.")
            }
        }
    }

    private fun createCountryOnlyFormElement(
        allowedCountryCodes: Set<String>,
    ): FormElement {
        return SectionElement.wrap(
            CountryElement(
                identifier = IdentifierSpec.Country,
                controller = DropdownFieldController(
                    config = CountryConfig(allowedCountryCodes),
                    initialValue = arguments.initialValues[IdentifierSpec.Country],
                ),
            ),
        )
    }

    private fun createTaxMinimumAddressFormElements(
        allowedCountryCodes: Set<String>,
    ): List<FormElement> {
        return AutomaticTaxBillingAddressFactory(arguments).create(
            allowedCountryCodes = allowedCountryCodes,
        )
    }

    private fun createFullAddressFormElements(
        allowedCountryCodes: Set<String>,
    ): List<FormElement> {
        return AddressSpec(allowedCountryCodes = allowedCountryCodes).transform(
            initialValues = arguments.initialValues,
            shippingValues = arguments.shippingValues,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
        )
    }
}

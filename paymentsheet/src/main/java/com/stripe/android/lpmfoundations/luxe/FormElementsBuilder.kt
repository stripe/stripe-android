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
    private val uiFormElements: MutableList<UiFormElement> = mutableListOf()
    private val footerFormElements: MutableList<FormElement> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> = mutableSetOf()
    private val overriddenContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()

    private var hasNormalAddressSource: Boolean = false
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

            uiFormElements += UiFormElement.Existing(type.formElement(arguments.initialValues))
        }
    }

    fun element(formElement: FormElement): FormElementsBuilder = apply {
        uiFormElements += UiFormElement.Existing(formElement)
    }

    fun countryOnly(
        allowedCountryCodes: Set<String>,
    ): FormElementsBuilder = apply {
        check(uiFormElements.none { it is UiFormElement.CountryOnly })
        uiFormElements += UiFormElement.CountryOnly(allowedCountryCodes)
    }

    fun ignoreBillingAddressRequirements() = apply {
        hasNormalAddressSource = false
        ignoreBillingAddressCollection = true
    }

    fun requireBillingAddressIfAllowed(
        availableCountries: Set<String> = this.availableCountries,
    ): FormElementsBuilder = apply {
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
        val resolvedUiFormElements = resolveBillingAddressSource()

        return buildList {
            addAll(headerFormElements) // Order headers first.

            for (collectionMode in requiredContactInformationCollectionModes) {
                if (collectionMode !in overriddenContactInformationCollectionModes) {
                    add(collectionMode.formElement(arguments.initialValues))
                }
            }

            addAll(
                resolvedUiFormElements.flatMap { item ->
                    item.createFormElements()
                }
            )

            addAll(footerFormElements) // Order footers last.
        }
    }

    private fun resolveBillingAddressSource(): List<UiFormElement> {
        val declaredElements = buildList {
            addAll(uiFormElements)
            if (hasNormalAddressSource) {
                add(UiFormElement.FullAddress(availableCountries))
            }
        }
        if (ignoreBillingAddressCollection) {
            return declaredElements
        }

        val countryOnly = declaredElements.filterIsInstance<UiFormElement.CountryOnly>().singleOrNull()
        val fullAddress = declaredElements.filterIsInstance<UiFormElement.FullAddress>().singleOrNull()
        val source = when {
            fullAddress != null -> BillingAddressResolver.Source.NormalAddress(
                hidesCountry = false,
            )
            countryOnly != null -> BillingAddressResolver.Source.CountryOnly(countryOnly.allowedCountryCodes)
            else -> BillingAddressResolver.Source.Absent
        }
        val result = BillingAddressResolver.resolve(
            source = source,
            addressCollectionMode = arguments.billingDetailsCollectionConfiguration.address,
            requiresBillingAddressForAutomaticTax = supportsAutomaticTaxBillingAddress &&
                arguments.requiresBillingAddressForAutomaticTax,
            allowedBillingCountries = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
        )

        return when (result) {
            BillingAddressResolver.Result.PreserveExisting -> declaredElements
            BillingAddressResolver.Result.UseExistingNormalAddress -> {
                declaredElements.filterNot { it is UiFormElement.CountryOnly }
            }
            is BillingAddressResolver.Result.CountryOnly -> {
                declaredElements.replaceCountryOrAppend(UiFormElement.CountryOnly(result.allowedCountryCodes))
            }
            is BillingAddressResolver.Result.TaxMinimum -> {
                declaredElements.replaceCountryOrAppend(UiFormElement.TaxMinimumAddress(result.allowedCountryCodes))
            }
            is BillingAddressResolver.Result.Full -> {
                declaredElements.replaceCountryOrAppend(UiFormElement.FullAddress(result.allowedCountryCodes))
            }
        }
    }

    private fun List<UiFormElement>.replaceCountryOrAppend(
        replacement: UiFormElement,
    ): List<UiFormElement> {
        val countryIndex = indexOfFirst { it is UiFormElement.CountryOnly }
        return if (countryIndex >= 0) {
            mapIndexedNotNull { index, item ->
                when {
                    index == countryIndex -> replacement
                    item is UiFormElement.CountryOnly -> null
                    else -> item
                }
            }
        } else {
            this + replacement
        }
    }

    private fun UiFormElement.createFormElements(): List<FormElement> {
        return when (this) {
            is UiFormElement.Existing -> listOf(formElement)
            is UiFormElement.CountryOnly -> {
                listOf(
                    SectionElement.wrap(
                        CountryElement(
                            identifier = IdentifierSpec.Country,
                            controller = DropdownFieldController(
                                config = CountryConfig(allowedCountryCodes),
                                initialValue = arguments.initialValues[IdentifierSpec.Country],
                            ),
                        )
                    )
                )
            }
            is UiFormElement.TaxMinimumAddress -> AutomaticTaxBillingAddressFactory(arguments).create(
                allowedCountryCodes = allowedCountryCodes,
            )
            is UiFormElement.FullAddress -> AddressSpec(allowedCountryCodes = allowedCountryCodes).transform(
                initialValues = arguments.initialValues,
                shippingValues = arguments.shippingValues,
                autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            )
        }
    }

    private sealed interface UiFormElement {
        data class Existing(
            val formElement: FormElement,
        ) : UiFormElement

        data class CountryOnly(
            val allowedCountryCodes: Set<String>,
        ) : UiFormElement

        data class TaxMinimumAddress(
            val allowedCountryCodes: Set<String>,
        ) : UiFormElement

        data class FullAddress(
            val allowedCountryCodes: Set<String>,
        ) : UiFormElement
    }
}

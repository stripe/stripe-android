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

    private var requireBillingAddressCollection: Boolean = false
    private var availableCountries: Set<String> =
        arguments.billingDetailsCollectionConfiguration.allowedBillingCountries

    init {
        // Setup the required contact information fields based on the merchant billingDetailsCollectionConfiguration.
        for (value in ContactInformationCollectionMode.entries) {
            if (value.isRequired(arguments.billingDetailsCollectionConfiguration)) {
                requireContactInformationIfAllowed(value)
            }
        }

        // Setup the required billing fields section based on the merchant billingDetailsCollectionConfiguration.
        if (arguments.billingDetailsCollectionConfiguration.address
            == PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        ) {
            requireBillingAddressIfAllowed()
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
        requireBillingAddressCollection = false
    }

    fun requireBillingAddressIfAllowed(
        availableCountries: Set<String> = this.availableCountries,
    ): FormElementsBuilder = apply {
        if (arguments.billingDetailsCollectionConfiguration.address
            != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        ) {
            requireBillingAddressCollection = true

            this.availableCountries = availableCountries
        }
    }

    fun footer(formElement: FormElement): FormElementsBuilder = apply {
        footerFormElements += formElement
    }

    fun build(): List<FormElement> {
        val shouldCollectTaxBillingAddress = supportsAutomaticTaxBillingAddress &&
            arguments.requiresBillingAddressForAutomaticTax &&
            arguments.billingDetailsCollectionConfiguration.address ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        val promotesCountryOnlyElement = shouldCollectTaxBillingAddress && !requireBillingAddressCollection

        return buildList {
            addAll(headerFormElements) // Order headers first.

            for (collectionMode in requiredContactInformationCollectionModes) {
                if (collectionMode !in overriddenContactInformationCollectionModes) {
                    add(collectionMode.formElement(arguments.initialValues))
                }
            }

            addAll(
                uiFormElements.flatMap { item ->
                    item.createFormElements(promotesCountryOnlyElement)
                }
            )

            if (requireBillingAddressCollection) {
                val elements = AddressSpec(allowedCountryCodes = availableCountries).transform(
                    initialValues = arguments.initialValues,
                    shippingValues = arguments.shippingValues,
                    autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
                )

                addAll(elements)
            }

            if (
                shouldCollectTaxBillingAddress &&
                !requireBillingAddressCollection &&
                uiFormElements.none { it is UiFormElement.CountryOnly }
            ) {
                addAll(
                    AutomaticTaxBillingAddressFactory(arguments).create(
                        allowedCountryCodes =
                        arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
                    )
                )
            }

            addAll(footerFormElements) // Order footers last.
        }
    }

    private fun UiFormElement.createFormElements(
        promotesCountryOnlyElement: Boolean,
    ): List<FormElement> {
        return when (this) {
            is UiFormElement.Existing -> listOf(formElement)
            is UiFormElement.CountryOnly -> {
                if (promotesCountryOnlyElement) {
                    AutomaticTaxBillingAddressFactory(arguments).create(
                        allowedCountryCodes = allowedCountryCodes,
                    )
                } else {
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
            }
        }
    }

    private sealed interface UiFormElement {
        data class Existing(
            val formElement: FormElement,
        ) : UiFormElement

        data class CountryOnly(
            val allowedCountryCodes: Set<String>,
        ) : UiFormElement
    }
}

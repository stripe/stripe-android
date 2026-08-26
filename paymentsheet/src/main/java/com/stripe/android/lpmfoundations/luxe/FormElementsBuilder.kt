package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormElement

internal class FormElementsBuilder(
    private val arguments: UiDefinitionFactory.Arguments,
    private val supportsAutomaticTaxBillingAddress: Boolean,
) {
    private val headerFormElements: MutableList<FormElement> = mutableListOf()
    private val uiFormElements: MutableList<FormElement> = mutableListOf()
    private val footerFormElements: MutableList<FormElement> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> = mutableSetOf()
    private val overriddenContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()

    private var requireBillingAddressCollection: Boolean = false
    private var availableCountries: Set<String> =
        arguments.billingDetailsCollectionConfiguration.allowedBillingCountries
    private var countryRequirement: CountryRequirement? = null

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

            uiFormElements += type.formElement(arguments.initialValues)
        }
    }

    fun element(formElement: FormElement): FormElementsBuilder = apply {
        uiFormElements += formElement
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

    fun requireCountry(
        allowedCountryCodes: Set<String>,
        initialValue: String?,
    ): FormElementsBuilder = apply {
        countryRequirement = CountryRequirement(
            allowedCountryCodes = allowedCountryCodes,
            initialValue = initialValue,
        )
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

            addAll(uiFormElements)

            addAll(
                BillingAddressFormElementsBuilder(
                    arguments = arguments,
                    supportsAutomaticTaxBillingAddress = supportsAutomaticTaxBillingAddress,
                    requireBillingAddressCollection = requireBillingAddressCollection,
                    fallbackCountryCodes = availableCountries,
                    countryRequirement = countryRequirement,
                ).build()
            )

            addAll(footerFormElements) // Order footers last.
        }
    }
}

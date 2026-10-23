package com.stripe.android.lpmfoundations

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet

@Suppress("TooManyFunctions") // Keep the form-building operations together in the builder API.
internal class FormElementsBuilder(
    private val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    private val requiresBillingAddressForAutomaticTax: Boolean,
    private val requiresMandate: Boolean,
    private val supportsAutomaticTaxBillingAddress: Boolean,
) {
    private val headerFormElements: MutableList<FormElementDefinition> = mutableListOf()
    private val uiFormElements: MutableList<FormElementDefinition> = mutableListOf()
    private val footerFormElements: MutableList<FormElementDefinition> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> = mutableSetOf()
    private val overriddenContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()

    private var requireBillingAddressCollection: Boolean = false
    private var availableCountries: Set<String> =
        billingDetailsCollectionConfiguration.allowedBillingCountries
    private var countryRequirement: CountryRequirementDefinition? = null

    init {
        // Setup the required contact information fields based on the merchant billingDetailsCollectionConfiguration.
        for (value in ContactInformationCollectionMode.entries) {
            if (value.isRequired(billingDetailsCollectionConfiguration)) {
                requireContactInformationIfAllowed(value)
            }
        }

        // Setup the required billing fields section based on the merchant billingDetailsCollectionConfiguration.
        if (billingDetailsCollectionConfiguration.address
            == PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        ) {
            requireBillingAddressIfAllowed()
        }
    }

    fun header(formElement: FormElementDefinition): FormElementsBuilder = apply {
        headerFormElements += formElement
    }

    fun ignoreContactInformationRequirements() = apply {
        requiredContactInformationCollectionModes.clear()
    }

    fun requireContactInformationIfAllowed(type: ContactInformationCollectionMode): FormElementsBuilder = apply {
        if (type.isAllowed(billingDetailsCollectionConfiguration)) {
            requiredContactInformationCollectionModes += type
        }
    }

    fun overrideContactInformationPosition(type: ContactInformationCollectionMode): FormElementsBuilder {
        return overrideContactInformationPosition(
            type = type,
            formElement = FormElementDefinition.interactive { type.formElement(it.initialValues) },
        )
    }

    fun overrideContactInformationPosition(
        type: ContactInformationCollectionMode,
        formElement: FormElementDefinition,
    ): FormElementsBuilder = apply {
        if (type in requiredContactInformationCollectionModes) {
            overriddenContactInformationCollectionModes += type

            uiFormElements += formElement
        }
    }

    fun element(formElement: FormElementDefinition): FormElementsBuilder = apply {
        uiFormElements += formElement
    }

    fun ignoreBillingAddressRequirements() = apply {
        requireBillingAddressCollection = false
    }

    fun requireBillingAddressIfAllowed(
        availableCountries: Set<String> = this.availableCountries,
    ): FormElementsBuilder = apply {
        if (billingDetailsCollectionConfiguration.address
            != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        ) {
            requireBillingAddressCollection = true

            this.availableCountries = availableCountries
        }
    }

    fun requireCountry(
        allowedCountryCodes: Set<String>,
        initialValue: (UiDefinitionFactory.Arguments) -> String?,
    ): FormElementsBuilder = apply {
        countryRequirement = CountryRequirementDefinition(
            allowedCountryCodes = allowedCountryCodes,
            initialValue = initialValue,
        )
    }

    fun addMandateIfAllowed(
        formElement: FormElementDefinition,
    ): FormElementsBuilder = apply {
        if (requiresMandate) {
            footer(formElement)
        }
    }

    fun footer(formElement: FormElementDefinition): FormElementsBuilder = apply {
        footerFormElements += formElement
    }

    fun build(): FormDefinition {
        return FormDefinition(
            buildList {
                addAll(headerFormElements) // Order headers first.

                for (collectionMode in requiredContactInformationCollectionModes) {
                    if (collectionMode !in overriddenContactInformationCollectionModes) {
                        add(FormElementDefinition.interactive { collectionMode.formElement(it.initialValues) })
                    }
                }

                addAll(uiFormElements)

                addAll(
                    BillingAddressFormElementsBuilder(
                        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                        requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
                        supportsAutomaticTaxBillingAddress = supportsAutomaticTaxBillingAddress,
                        requireBillingAddressCollection = requireBillingAddressCollection,
                        fallbackCountryCodes = availableCountries,
                        countryRequirement = countryRequirement,
                    ).build()
                )

                addAll(footerFormElements) // Order footers last.
            }
        )
    }
}

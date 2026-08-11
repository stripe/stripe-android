package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement

internal class FormElementsBuilder(
    private val arguments: UiDefinitionFactory.Arguments,
) {
    private val headerFormElements: MutableList<FormElement> = mutableListOf()
    private val countryOnlyHeaderFormElements: MutableList<FormElement> = mutableListOf()
    private val uiFormElements: MutableList<FormElement> = mutableListOf()
    private val footerFormElements: MutableList<FormElement> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()
    private val overriddenContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()

    private var requireBillingAddressCollection: Boolean = false
    private val addressCollectionMode = arguments.billingDetailsCollectionConfiguration.address
    private var availableCountries: Set<String> =
        arguments.billingDetailsCollectionConfiguration.allowedBillingCountries
    private var countryOnlyBillingAddress: CountryOnlyBillingAddress? = null

    init {
        // Setup the required contact information fields based on the merchant billingDetailsCollectionConfiguration.
        for (value in ContactInformationCollectionMode.entries) {
            if (value.isRequired(arguments.billingDetailsCollectionConfiguration)) {
                requireContactInformationIfAllowed(value)
            }
        }

        // Setup the required billing fields section based on the merchant billingDetailsCollectionConfiguration.
        if (addressCollectionMode
            == PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        ) {
            requireBillingAddressIfAllowed()
        }
    }

    fun header(formElement: FormElement): FormElementsBuilder = apply {
        headerFormElements += formElement
    }

    fun countryOnlyHeader(formElement: FormElement): FormElementsBuilder = apply {
        countryOnlyHeaderFormElements += formElement
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

    fun countryOnly(
        allowedCountryCodes: Set<String>,
        initialValue: String?,
    ): FormElementsBuilder = apply {
        check(countryOnlyBillingAddress == null) {
            "Only one country-only billing address source can be declared."
        }
        countryOnlyBillingAddress = CountryOnlyBillingAddress(
            allowedCountryCodes = allowedCountryCodes,
            initialValue = initialValue,
        )
    }

    fun ignoreBillingAddressRequirements() = apply {
        requireBillingAddressCollection = false
    }

    fun requireBillingAddressIfAllowed(
        availableCountries: Set<String> = this.availableCountries,
    ): FormElementsBuilder = apply {
        if (addressCollectionMode
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
        return buildList {
            addAll(headerFormElements) // Order headers first.
            if (isCountryOnlyCollection()) {
                addAll(countryOnlyHeaderFormElements)
            }

            for (collectionMode in requiredContactInformationCollectionModes) {
                if (collectionMode !in overriddenContactInformationCollectionModes) {
                    add(collectionMode.formElement(arguments.initialValues))
                }
            }

            addAll(uiFormElements)
            addAll(
                createBillingAddressElements(
                    countryOnlyBillingAddress = countryOnlyBillingAddress,
                    addressCollectionMode = addressCollectionMode,
                    requireBillingAddressCollection = requireBillingAddressCollection,
                ),
            )

            addAll(footerFormElements) // Order footers last.
        }
    }

    private fun createBillingAddressElements(
        countryOnlyBillingAddress: CountryOnlyBillingAddress?,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requireBillingAddressCollection: Boolean,
    ): List<FormElement> {
        val collectionMode = when {
            isCountryOnlyCollection(
                countryOnlyBillingAddress = countryOnlyBillingAddress,
                addressCollectionMode = addressCollectionMode,
            ) -> {
                BillingAddressCollectionMode.Country(emptyMap())
            }
            countryOnlyBillingAddress != null || requireBillingAddressCollection -> {
                BillingAddressCollectionMode.Full
            }
            else -> return emptyList()
        }

        return createBillingAddressElements(
            collectionMode = collectionMode,
            allowedCountryCodes = countryOnlyBillingAddress?.allowedCountryCodes ?: availableCountries,
            initialCountry = countryOnlyBillingAddress?.initialValue,
        )
    }

    private fun isCountryOnlyCollection(): Boolean {
        return isCountryOnlyCollection(
            countryOnlyBillingAddress = countryOnlyBillingAddress,
            addressCollectionMode = addressCollectionMode,
        )
    }

    private fun isCountryOnlyCollection(
        countryOnlyBillingAddress: CountryOnlyBillingAddress?,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
    ): Boolean {
        return countryOnlyBillingAddress != null &&
            addressCollectionMode !=
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
    }

    private fun createBillingAddressElements(
        collectionMode: BillingAddressCollectionMode,
        allowedCountryCodes: Set<String>,
        initialCountry: String?,
    ): List<FormElement> {
        val sameAsShippingElement = collectionMode.takeIf {
            it == BillingAddressCollectionMode.Full
        }?.let {
            arguments.shippingValues?.get(IdentifierSpec.SameAsShipping)
                ?.toBooleanStrictOrNull()
                ?.let { isSameAsShipping ->
                    SameAsShippingElement(
                        identifier = IdentifierSpec.SameAsShipping,
                        controller = SameAsShippingController(isSameAsShipping),
                    )
                }
        }
        val rawValues = initialCountry?.let {
            arguments.initialValues + (IdentifierSpec.Country to it)
        } ?: arguments.initialValues
        val billingAddressElement = BillingAddressElement(
            identifier = when (collectionMode) {
                is BillingAddressCollectionMode.Country -> IdentifierSpec.Country
                BillingAddressCollectionMode.Full,
                BillingAddressCollectionMode.Never -> IdentifierSpec.BillingAddress
            },
            rawValuesMap = rawValues,
            countryCodes = allowedCountryCodes,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = arguments.shippingValues,
            addressCollectionMode = collectionMode,
            shouldHideCountryOnNoAddressCollection = false,
        )

        return listOfNotNull(
            SectionElement.wrap(
                sectionFieldElement = billingAddressElement,
                label = if (collectionMode == BillingAddressCollectionMode.Full) {
                    R.string.stripe_billing_details.resolvableString
                } else {
                    null
                },
            ),
            sameAsShippingElement,
        )
    }

    private data class CountryOnlyBillingAddress(
        val allowedCountryCodes: Set<String>,
        val initialValue: String?,
    )
}

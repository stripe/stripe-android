package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal class CountrySpecFormElementFactory(
    private val arguments: UiDefinitionFactory.Arguments,
    private val initialCountry: String?,
) {
    constructor(arguments: UiDefinitionFactory.Arguments) : this(
        arguments = arguments,
        initialCountry = null,
    )

    fun transform(spec: CountrySpec): List<FormElement> {
        val initialValues = arguments.initialValues.toMutableMap().apply {
            initialCountry?.let { put(spec.apiPath, it) }
        }

        return listOf(createCountryOnlySection(spec, initialValues))
    }

    private fun createCountryOnlySection(
        spec: CountrySpec,
        initialValues: Map<IdentifierSpec, String?>,
    ): SectionElement {
        return SectionElement.wrap(
            createBillingAddressElement(
                spec = spec,
                initialValues = initialValues,
            ),
        )
    }

    private fun createBillingAddressElement(
        spec: CountrySpec,
        initialValues: Map<IdentifierSpec, String?>,
    ): BillingAddressElement {
        return BillingAddressElement(
            identifier = spec.apiPath,
            rawValuesMap = initialValues,
            countryCodes = spec.allowedCountryCodes,
            countryDropdownFieldController = DropdownFieldController(
                CountryConfig(spec.allowedCountryCodes),
                initialValue = initialValues[spec.apiPath],
            ),
            countryElementIdentifier = spec.apiPath,
            autocompleteAddressInteractorFactory = null,
            sameAsShippingElement = null,
            shippingValuesMap = null,
            addressCollectionMode = BillingAddressCollectionMode.Country(emptyMap()),
            collectionConfiguration = BillingDetailsCollectionConfiguration(
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                allowedCountries = spec.allowedCountryCodes,
            ),
        )
    }
}

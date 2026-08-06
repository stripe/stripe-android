package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal fun CountrySpec.toCountryOnlyBillingAddressSection(
    initialValues: Map<IdentifierSpec, String?>,
    initialCountry: String?,
): SectionElement {
    val countryInitialValues = initialValues.toMutableMap().apply {
        initialCountry?.let { put(IdentifierSpec.Country, it) }
    }
    return SectionElement.wrap(
        BillingAddressElement(
            configuration = BillingAddressElement.Configuration(
                identifier = IdentifierSpec.Country,
                initialValues = countryInitialValues,
                countryCodes = allowedCountryCodes,
                autocompleteAddressInteractorFactory = null,
                shippingValues = null,
                addressCollectionMode = BillingAddressCollectionMode.Country(emptyMap()),
                collectionConfiguration = BillingDetailsCollectionConfiguration(
                    address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                    allowedCountries = allowedCountryCodes,
                ),
                shouldHideCountryOnNoAddressCollection = true,
            ),
            countryDropdownFieldController = DropdownFieldController(
                CountryConfig(allowedCountryCodes),
                initialValue = countryInitialValues[IdentifierSpec.Country],
            ),
            sameAsShippingElement = null,
        ),
    )
}

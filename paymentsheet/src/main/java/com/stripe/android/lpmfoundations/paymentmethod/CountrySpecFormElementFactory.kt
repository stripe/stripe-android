package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
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

        return when (arguments.billingDetailsCollectionConfiguration.address) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                createFullAddressElements(spec, initialValues)
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                listOf(createCountryOnlySection(spec, initialValues))
            }
        }
    }

    private fun createCountryOnlySection(
        spec: CountrySpec,
        initialValues: Map<IdentifierSpec, String?>,
    ): SectionElement {
        return SectionElement.wrap(
            createBillingAddressElement(
                spec = spec,
                initialValues = initialValues,
                addressCollectionMode = BillingAddressCollectionMode.Country(emptyMap()),
                sameAsShippingElement = null,
            ),
        )
    }

    private fun createFullAddressElements(
        spec: CountrySpec,
        initialValues: Map<IdentifierSpec, String?>,
    ): List<FormElement> {
        val sameAsShippingElement = arguments.shippingValues
            ?.get(IdentifierSpec.SameAsShipping)
            ?.toBooleanStrictOrNull()
            ?.let { isSameAsShipping ->
                SameAsShippingElement(
                    identifier = IdentifierSpec.SameAsShipping,
                    controller = SameAsShippingController(isSameAsShipping),
                )
            }

        return listOfNotNull(
            SectionElement.wrap(
                createBillingAddressElement(
                    spec = spec,
                    initialValues = initialValues,
                    addressCollectionMode = BillingAddressCollectionMode.Full,
                    sameAsShippingElement = sameAsShippingElement,
                ),
                R.string.stripe_billing_details.resolvableString,
            ),
            sameAsShippingElement,
        )
    }

    private fun createBillingAddressElement(
        spec: CountrySpec,
        initialValues: Map<IdentifierSpec, String?>,
        addressCollectionMode: BillingAddressCollectionMode,
        sameAsShippingElement: SameAsShippingElement?,
    ): BillingAddressElement {
        return BillingAddressElement(
            identifier = if (addressCollectionMode == BillingAddressCollectionMode.Full) {
                IdentifierSpec.BillingAddress
            } else {
                spec.apiPath
            },
            rawValuesMap = initialValues,
            countryCodes = spec.allowedCountryCodes,
            countryDropdownFieldController = DropdownFieldController(
                CountryConfig(spec.allowedCountryCodes),
                initialValue = initialValues[spec.apiPath],
            ),
            countryElementIdentifier = spec.apiPath,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = arguments.shippingValues,
            addressCollectionMode = addressCollectionMode,
            collectionConfiguration = BillingDetailsCollectionConfiguration(
                address = if (addressCollectionMode == BillingAddressCollectionMode.Full) {
                    BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                } else {
                    BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
                },
                allowedCountries = spec.allowedCountryCodes,
            ),
        )
    }
}

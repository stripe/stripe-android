package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.additionalAutomaticTaxFieldsByCountry
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement

internal class BillingAddressFormElementsBuilder(
    private val arguments: UiDefinitionFactory.Arguments,
    private val supportsAutomaticTaxBillingAddress: Boolean,
    private val requireBillingAddressCollection: Boolean,
    private val fallbackCountryCodes: Set<String>,
    private val countryRequirement: CountryRequirement?,
) {
    private val resolvedInitialValues = countryRequirement?.applyTo(arguments.initialValues) ?: arguments.initialValues
    private val fullAddressCountryCodes = countryRequirement?.allowedCountryCodes ?: fallbackCountryCodes
    private val automaticTaxCountryCodes = countryRequirement?.allowedCountryCodes
        ?: arguments.billingDetailsCollectionConfiguration.allowedBillingCountries

    fun build(): List<FormElement> {
        return when {
            requireBillingAddressCollection -> createAddressElements()
            shouldCollectAutomaticTaxBillingAddress() -> createAutomaticTaxBillingAddressElements()
            countryRequirement != null -> listOf(createCountryElement(countryRequirement))
            else -> emptyList()
        }
    }

    private fun shouldCollectAutomaticTaxBillingAddress(): Boolean {
        return supportsAutomaticTaxBillingAddress &&
            arguments.billingDetailsCollectionConfiguration.address ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic &&
            arguments.requiresBillingAddressForAutomaticTax &&
            !requireBillingAddressCollection
    }

    private fun createCountryElement(requirement: CountryRequirement): FormElement {
        return SectionElement.wrap(
            CountryElement(
                identifier = IdentifierSpec.Country,
                controller = DropdownFieldController(
                    config = CountryConfig(requirement.allowedCountryCodes),
                    initialValue = requirement.initialValue,
                )
            )
        )
    }

    private fun createAddressElements(): List<FormElement> {
        return AddressSpec(
            allowedCountryCodes = fullAddressCountryCodes,
        ).transform(
            initialValues = resolvedInitialValues,
            shippingValues = arguments.shippingValues,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
        )
    }

    private fun createAutomaticTaxBillingAddressElements(): List<FormElement> {
        val sameAsShippingElement = arguments.shippingValues
            ?.get(IdentifierSpec.SameAsShipping)
            ?.toBooleanStrictOrNull()
            ?.let { sameAsShipping ->
                SameAsShippingElement(
                    identifier = IdentifierSpec.SameAsShipping,
                    controller = SameAsShippingController(sameAsShipping),
                )
            }
        val billingAddressElement = BillingAddressElement(
            identifier = IdentifierSpec.BillingAddress,
            rawValuesMap = resolvedInitialValues,
            countryCodes = automaticTaxCountryCodes,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = arguments.shippingValues,
            addressCollectionMode = BillingAddressCollectionMode.Country(
                additionalFieldsByCountry = additionalAutomaticTaxFieldsByCountry,
            ),
        )

        return listOfNotNull(
            SectionElement.wrap(billingAddressElement),
            sameAsShippingElement,
        )
    }
}

internal data class CountryRequirement(
    val allowedCountryCodes: Set<String>,
    val initialValue: String?,
) {
    fun applyTo(initialValues: Map<IdentifierSpec, String?>): Map<IdentifierSpec, String?> {
        return initialValues + (IdentifierSpec.Country to initialValue)
    }
}

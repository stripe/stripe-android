package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal class BillingAddressFormElementsBuilder(
    private val arguments: UiDefinitionFactory.Arguments,
    private val requireBillingAddressCollection: Boolean,
    private val fallbackCountryCodes: Set<String>,
    private val countryRequirement: CountryRequirement?,
) {
    private val resolvedInitialValues = countryRequirement?.applyTo(arguments.initialValues) ?: arguments.initialValues
    private val fullAddressCountryCodes = countryRequirement?.allowedCountryCodes ?: fallbackCountryCodes

    fun build(): List<FormElement> {
        return when {
            requireBillingAddressCollection -> createAddressElements()
            countryRequirement != null -> listOf(createCountryElement(countryRequirement))
            else -> emptyList()
        }
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
}

internal data class CountryRequirement(
    val allowedCountryCodes: Set<String>,
    val initialValue: String?,
) {
    fun applyTo(initialValues: Map<IdentifierSpec, String?>): Map<IdentifierSpec, String?> {
        return initialValues + (IdentifierSpec.Country to initialValue)
    }
}

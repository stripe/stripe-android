package com.stripe.android.lpmfoundations

import androidx.compose.ui.autofill.ContentType
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.additionalAutomaticTaxFieldsByCountry
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement

internal class BillingAddressFormElementsBuilder(
    private val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    private val requiresBillingAddressForAutomaticTax: Boolean,
    private val supportsAutomaticTaxBillingAddress: Boolean,
    private val requireBillingAddressCollection: Boolean,
    private val fallbackCountryCodes: Set<String>,
    private val countryRequirement: CountryRequirementDefinition?,
) {
    private val fullAddressCountryCodes = countryRequirement?.allowedCountryCodes ?: fallbackCountryCodes
    private val automaticTaxCountryCodes = countryRequirement?.allowedCountryCodes
        ?: billingDetailsCollectionConfiguration.allowedBillingCountries

    fun build(): List<FormElementDefinition> {
        val factory: ((UiDefinitionFactory.Arguments) -> List<FormElement>) = when {
            requireBillingAddressCollection -> ::createAddressElements
            shouldCollectAutomaticTaxBillingAddress() -> ::createAutomaticTaxBillingAddressElements
            countryRequirement != null -> { arguments ->
                listOf(createCountryElement(countryRequirement.resolve(arguments)))
            }
            else -> return emptyList()
        }
        return listOf(FormElementDefinition(true, null, factory))
    }

    private fun resolvedInitialValues(arguments: UiDefinitionFactory.Arguments): Map<FormFieldId, String?> {
        return countryRequirement?.resolve(arguments)?.applyTo(arguments.initialValues) ?: arguments.initialValues
    }

    private fun shouldCollectAutomaticTaxBillingAddress(): Boolean {
        return supportsAutomaticTaxBillingAddress &&
            billingDetailsCollectionConfiguration.address ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic &&
            requiresBillingAddressForAutomaticTax &&
            !requireBillingAddressCollection
    }

    private fun createCountryElement(requirement: CountryRequirement): FormElement {
        return SectionElement.wrap(
            CountryElement(
                identifier = FormFieldId.Country,
                controller = DropdownFieldController(
                    config = CountryConfig(requirement.allowedCountryCodes, autofillType = ContentType.AddressCountry),
                    initialValue = requirement.initialValue,
                )
            )
        )
    }

    private fun createAddressElements(arguments: UiDefinitionFactory.Arguments): List<FormElement> {
        val sameAsShippingElement = arguments.shippingValues
            ?.get(FormFieldId.SameAsShipping)
            ?.toBooleanStrictOrNull()
            ?.let { sameAsShipping ->
                SameAsShippingElement(
                    identifier = FormFieldId.SameAsShipping,
                    controller = SameAsShippingController(sameAsShipping),
                )
            }
        val addressElement = arguments.autocompleteAddressInteractorFactory?.let { interactorFactory ->
            AutocompleteAddressElement(
                identifier = FormFieldId.BillingAddress,
                initialValues = resolvedInitialValues(arguments),
                countryCodes = fullAddressCountryCodes,
                sameAsShippingElement = sameAsShippingElement,
                shippingValuesMap = arguments.shippingValues,
                hideCountry = false,
                interactorFactory = interactorFactory,
            )
        } ?: AddressElement(
            _identifier = FormFieldId.BillingAddress,
            rawValuesMap = resolvedInitialValues(arguments),
            countryCodes = fullAddressCountryCodes,
            addressInputMode = AddressInputMode.NoAutocomplete(),
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = arguments.shippingValues,
            hideCountry = false,
        )

        return listOfNotNull(
            SectionElement.wrap(addressElement, resolvableString(R.string.stripe_billing_details)),
            sameAsShippingElement,
        )
    }

    private fun createAutomaticTaxBillingAddressElements(arguments: UiDefinitionFactory.Arguments): List<FormElement> {
        val sameAsShippingElement = arguments.shippingValues
            ?.get(FormFieldId.SameAsShipping)
            ?.toBooleanStrictOrNull()
            ?.let { sameAsShipping ->
                SameAsShippingElement(
                    identifier = FormFieldId.SameAsShipping,
                    controller = SameAsShippingController(sameAsShipping),
                )
            }
        val billingAddressElement = BillingAddressElement(
            identifier = FormFieldId.BillingAddress,
            rawValuesMap = resolvedInitialValues(arguments),
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
    fun applyTo(initialValues: Map<FormFieldId, String?>): Map<FormFieldId, String?> {
        return initialValues + (FormFieldId.Country to initialValue)
    }
}

internal data class CountryRequirementDefinition(
    val allowedCountryCodes: Set<String>,
    val initialValue: (UiDefinitionFactory.Arguments) -> String?,
) {
    fun resolve(arguments: UiDefinitionFactory.Arguments): CountryRequirement {
        return CountryRequirement(allowedCountryCodes, initialValue(arguments))
    }
}

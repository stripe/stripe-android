package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.additionalAutomaticTaxFieldsByCountry
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.elements.SectionElement

/**
 * Widens an existing shared address for automatic tax, or appends a country-first billing
 * address when the payment method does not already collect one.
 */
internal fun List<FormElement>.withAutomaticTaxBillingAddressIfNecessary(
    paymentMethodCode: String,
    arguments: UiDefinitionFactory.Arguments,
): List<FormElement> {
    val shouldCollectTaxAddress = arguments.requiresBillingAddressForAutomaticTax &&
        arguments.billingDetailsCollectionConfiguration.address ==
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
    if (!shouldCollectTaxAddress || paymentMethodCode in separatelyRenderedPaymentMethodCodes) {
        return this
    }

    val addressField = filterIsInstance<SectionElement>()
        .flatMap { it.fields }
        .filterIsInstance<AddressFieldsElement>()
        .firstOrNull()
    if (addressField != null && addressField !is BillingAddressElement) {
        return this
    }

    val existingSameAsShippingElement = filterIsInstance<SameAsShippingElement>().firstOrNull()
    val sameAsShippingElement = existingSameAsShippingElement ?: createSameAsShippingElement(arguments)

    if (addressField is BillingAddressElement) {
        return widenBillingAddress(
            addressField = addressField,
            sameAsShippingElement = sameAsShippingElement,
            appendSameAsShippingElement = existingSameAsShippingElement == null,
        )
    }

    val addressElement = createTaxBillingAddressElement(
        arguments = arguments,
        countryDropdownFieldController = DropdownFieldController(
            config = CountryConfig(arguments.billingDetailsCollectionConfiguration.allowedBillingCountries),
            initialValue = arguments.initialValues[IdentifierSpec.Country],
        ),
        sameAsShippingElement = sameAsShippingElement,
    )

    return this + listOfNotNull(
        SectionElement.wrap(addressElement, R.string.stripe_billing_details.resolvableString),
        sameAsShippingElement,
    )
}

private fun List<FormElement>.widenBillingAddress(
    addressField: BillingAddressElement,
    sameAsShippingElement: SameAsShippingElement?,
    appendSameAsShippingElement: Boolean,
): List<FormElement> {
    val addressSectionIndex = indexOfFirst { element ->
        element is SectionElement && addressField in element.fields
    }
    val addressSection = this[addressSectionIndex] as SectionElement
    val widenedFields = addressSection.fields.map { field ->
        if (field === addressField) {
            addressField.withAdditionalFields(
                additionalFieldsByCountry = additionalAutomaticTaxFieldsByCountry,
                sameAsShippingElement = sameAsShippingElement,
            )
        } else {
            field
        }
    }
    val widenedSection = SectionElement(
        identifier = addressSection.identifier,
        fields = widenedFields,
        controller = SectionController(
            label = addressSection.controller.label,
            sectionFieldValidationControllers = widenedFields.map { it.sectionFieldErrorController() },
        ),
    )
    return flatMapIndexed { index, element ->
        if (index == addressSectionIndex) {
            listOfNotNull(
                widenedSection,
                sameAsShippingElement.takeIf { appendSameAsShippingElement },
            )
        } else {
            listOf(element)
        }
    }
}

private fun createTaxBillingAddressElement(
    arguments: UiDefinitionFactory.Arguments,
    countryDropdownFieldController: DropdownFieldController,
    sameAsShippingElement: SameAsShippingElement?,
): BillingAddressElement {
    return BillingAddressElement(
        configuration = BillingAddressElement.Configuration(
            identifier = IdentifierSpec.Generic("billing_details[address]"),
            initialValues = arguments.initialValues,
            countryCodes = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
            countryElementIdentifier = IdentifierSpec.Country,
            autocompleteAddressInteractorFactory = null,
            shippingValues = arguments.shippingValues,
            addressCollectionMode = BillingAddressCollectionMode.Country(
                additionalFieldsByCountry = additionalAutomaticTaxFieldsByCountry,
            ),
            collectionConfiguration = BillingDetailsCollectionConfiguration(
                collectName = false,
                collectEmail = false,
                collectPhone = false,
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                allowedCountries = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
            ),
            shouldHideCountryOnNoAddressCollection = false,
        ),
        countryDropdownFieldController = countryDropdownFieldController,
        sameAsShippingElement = sameAsShippingElement,
    )
}

private fun createSameAsShippingElement(
    arguments: UiDefinitionFactory.Arguments,
): SameAsShippingElement? {
    return arguments.shippingValues
        ?.get(IdentifierSpec.SameAsShipping)
        ?.toBooleanStrictOrNull()
        ?.let { isSameAsShipping ->
            SameAsShippingElement(
                identifier = IdentifierSpec.SameAsShipping,
                controller = SameAsShippingController(isSameAsShipping),
            )
        }
}

private val separatelyRenderedPaymentMethodCodes = setOf(
    PaymentMethod.Type.USBankAccount.code,
    PaymentMethod.Type.Link.code,
)

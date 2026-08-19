package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AutomaticTaxBillingAddressSpec
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

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

    private var paymentMethodBillingAddressPolicy: PaymentMethodBillingAddressPolicy? = null

    init {
        // Setup the required contact information fields based on the merchant billingDetailsCollectionConfiguration.
        for (value in ContactInformationCollectionMode.entries) {
            if (value.isRequired(arguments.billingDetailsCollectionConfiguration)) {
                requireContactInformationIfAllowed(value)
            }
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

    fun billingAddressPolicy(policy: PaymentMethodBillingAddressPolicy): FormElementsBuilder = apply {
        paymentMethodBillingAddressPolicy = policy
    }

    fun footer(formElement: FormElement): FormElementsBuilder = apply {
        footerFormElements += formElement
    }

    fun build(): List<FormElement> {
        val resolvedBillingAddress = BillingAddressPolicyResolver(
            policy = paymentMethodBillingAddressPolicy,
            addressCollectionMode = arguments.billingDetailsCollectionConfiguration.address,
            requiresBillingAddressForAutomaticTax = supportsAutomaticTaxBillingAddress &&
                arguments.requiresBillingAddressForAutomaticTax,
            merchantAllowedCountryCodes =
                arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
        ).resolve()
        val billingAddressFormElements = resolvedBillingAddress.createFormElements()

        return buildList {
            addAll(headerFormElements) // Order headers first.

            for (collectionMode in requiredContactInformationCollectionModes) {
                if (collectionMode !in overriddenContactInformationCollectionModes) {
                    add(collectionMode.formElement(arguments.initialValues))
                }
            }

            addAll(uiFormElements)
            addAll(billingAddressFormElements)
            addAll(footerFormElements) // Order footers last.
        }
    }

    private fun ResolvedBillingAddress.createFormElements(): List<FormElement> {
        return when (this) {
            ResolvedBillingAddress.Absent,
            ResolvedBillingAddress.Suppressed -> emptyList()
            is ResolvedBillingAddress.CountryOnly -> listOf(
                SectionElement.wrap(
                    sectionFieldElement = CountryElement(
                        identifier = IdentifierSpec.Country,
                        controller = DropdownFieldController(
                            config = CountryConfig(allowedCountryCodes),
                            initialValue = arguments.initialValues[IdentifierSpec.Country],
                        ),
                    ),
                ),
            )
            is ResolvedBillingAddress.Full -> AddressSpec(
                allowedCountryCodes = allowedCountryCodes,
            ).transform(
                initialValues = arguments.initialValues,
                shippingValues = arguments.shippingValues,
                autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            )
            is ResolvedBillingAddress.TaxMinimum -> AutomaticTaxBillingAddressFactory(arguments).create(
                spec = AutomaticTaxBillingAddressSpec(allowedCountryCodes),
            )
        }
    }
}

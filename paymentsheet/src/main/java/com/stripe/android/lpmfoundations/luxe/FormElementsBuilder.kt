package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.additionalAutomaticTaxFieldsByCountry
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement

internal class FormElementsBuilder(
    private val arguments: UiDefinitionFactory.Arguments,
    private val allowsBillingAddressForAutomaticTax: Boolean,
) {
    private val headerFormElements: MutableList<FormElement> = mutableListOf()
    private val uiFormElements: MutableList<FormElement> = mutableListOf()
    private val footerFormElements: MutableList<FormElement> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> = mutableSetOf()
    private val overriddenContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()

    private var billingAddressSource: BillingAddressSource = BillingAddressSource.None

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

    fun countryOnly(
        allowedCountryCodes: Set<String>,
        initialValue: String?,
    ) = apply {
        billingAddressSource = BillingAddressSource.CountryOnly(
            allowedCountryCodes = allowedCountryCodes,
            initialValue = initialValue,
        )
    }

    fun suppressBillingAddressRequirements() = apply {
        billingAddressSource = BillingAddressSource.Suppressed
    }

    fun requireBillingAddressIfAllowed(
        availableCountries: Set<String>,
    ): FormElementsBuilder = apply {
        billingAddressSource = BillingAddressSource.FullIfAllowed(availableCountries)
    }

    fun footer(formElement: FormElement): FormElementsBuilder = apply {
        footerFormElements += formElement
    }

    fun build(): List<FormElement> {
        return buildList {
            addAll(headerFormElements) // Order headers first.

            for (collectionMode in requiredContactInformationCollectionModes) {
                if (collectionMode !in overriddenContactInformationCollectionModes) {
                    add(collectionMode.formElement(arguments.initialValues))
                }
            }

            addAll(uiFormElements)

            addAll(createBillingAddressElements())

            addAll(footerFormElements) // Order footers last.
        }
    }

    private fun createBillingAddressElements(): List<FormElement> {
        val resolved = resolveBillingAddress() ?: return emptyList()
        val sameAsShippingElement = resolved.collectionMode.takeIf {
            it != BillingAddressCollectionMode.Country(emptyMap())
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
        val rawValues = resolved.initialCountry?.let {
            arguments.initialValues + (IdentifierSpec.Country to it)
        } ?: arguments.initialValues
        val billingAddressElement = BillingAddressElement(
            identifier = IdentifierSpec.BillingAddress,
            rawValuesMap = rawValues,
            countryCodes = resolved.allowedCountryCodes,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = arguments.shippingValues,
            addressCollectionMode = resolved.collectionMode,
            shouldHideCountryOnNoAddressCollection = false,
        )

        return listOfNotNull(
            SectionElement.wrap(
                sectionFieldElement = billingAddressElement,
                label = if (resolved.collectionMode == BillingAddressCollectionMode.Country(emptyMap())) {
                    null
                } else {
                    R.string.stripe_billing_details.resolvableString
                },
            ),
            sameAsShippingElement,
        )
    }

    private fun resolveBillingAddress(): ResolvedBillingAddress? {
        val merchantMode = arguments.billingDetailsCollectionConfiguration.address
        val requiresTaxAddress = allowsBillingAddressForAutomaticTax &&
            arguments.requiresBillingAddressForAutomaticTax
        val sourceCountries = when (val source = billingAddressSource) {
            is BillingAddressSource.CountryOnly -> source.allowedCountryCodes
            is BillingAddressSource.FullIfAllowed -> source.allowedCountryCodes
            BillingAddressSource.None,
            BillingAddressSource.Suppressed -> arguments.billingDetailsCollectionConfiguration.allowedBillingCountries
        }
        val initialCountry = (billingAddressSource as? BillingAddressSource.CountryOnly)?.initialValue

        val collectionMode = resolveCollectionMode(merchantMode, requiresTaxAddress) ?: return null

        return ResolvedBillingAddress(
            collectionMode = collectionMode,
            allowedCountryCodes = sourceCountries,
            initialCountry = initialCountry,
        )
    }

    private fun resolveCollectionMode(
        merchantMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        requiresTaxAddress: Boolean,
    ): BillingAddressCollectionMode? {
        return when (merchantMode) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> resolveNever()
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
                resolveAutomatic(requiresTaxAddress)
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                resolveFull(requiresTaxAddress)
            }
        }
    }

    private fun resolveNever(): BillingAddressCollectionMode? {
        return when (billingAddressSource) {
            is BillingAddressSource.CountryOnly -> BillingAddressCollectionMode.Country(emptyMap())
            BillingAddressSource.None,
            is BillingAddressSource.FullIfAllowed,
            BillingAddressSource.Suppressed -> null
        }
    }

    private fun resolveAutomatic(requiresTaxAddress: Boolean): BillingAddressCollectionMode? {
        return when (billingAddressSource) {
            is BillingAddressSource.FullIfAllowed -> BillingAddressCollectionMode.Full
            is BillingAddressSource.CountryOnly -> if (requiresTaxAddress) {
                BillingAddressCollectionMode.Country(additionalAutomaticTaxFieldsByCountry)
            } else {
                BillingAddressCollectionMode.Country(emptyMap())
            }
            BillingAddressSource.None,
            BillingAddressSource.Suppressed -> if (requiresTaxAddress) {
                BillingAddressCollectionMode.Country(additionalAutomaticTaxFieldsByCountry)
            } else {
                null
            }
        }
    }

    private fun resolveFull(requiresTaxAddress: Boolean): BillingAddressCollectionMode? {
        return when (billingAddressSource) {
            BillingAddressSource.Suppressed -> if (requiresTaxAddress) {
                BillingAddressCollectionMode.Country(additionalAutomaticTaxFieldsByCountry)
            } else {
                null
            }
            BillingAddressSource.None,
            is BillingAddressSource.CountryOnly,
            is BillingAddressSource.FullIfAllowed -> BillingAddressCollectionMode.Full
        }
    }

    private sealed interface BillingAddressSource {
        data object None : BillingAddressSource

        data class CountryOnly(
            val allowedCountryCodes: Set<String>,
            val initialValue: String?,
        ) : BillingAddressSource

        data class FullIfAllowed(
            val allowedCountryCodes: Set<String>,
        ) : BillingAddressSource

        data object Suppressed : BillingAddressSource
    }

    private data class ResolvedBillingAddress(
        val collectionMode: BillingAddressCollectionMode,
        val allowedCountryCodes: Set<String>,
        val initialCountry: String?,
    )
}

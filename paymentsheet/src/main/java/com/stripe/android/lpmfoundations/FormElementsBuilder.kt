package com.stripe.android.lpmfoundations

import androidx.compose.ui.unit.Dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId

/** Registers deferred factories; building and inspecting a form never resolves its elements. */
@Suppress("TooManyFunctions") // Keep the form-building operations together in the builder API.
internal class FormElementsBuilder(
    private val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    private val requiresBillingAddressForAutomaticTax: Boolean,
    private val requiresMandate: Boolean,
    private val supportsAutomaticTaxBillingAddress: Boolean,
) {
    private val headerFormElements: MutableList<FormElementDefinition> = mutableListOf()
    private val uiFormElements: MutableList<FormElementDefinition> = mutableListOf()
    private val footerFormElements: MutableList<FormElementDefinition> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> = mutableSetOf()
    private val overriddenContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> =
        mutableSetOf()

    private var requireBillingAddressCollection: Boolean = false
    private var availableCountries: Set<String> =
        billingDetailsCollectionConfiguration.allowedBillingCountries
    private var countryRequirement: CountryRequirementDefinition? = null

    init {
        // Setup the required contact information fields based on the merchant billingDetailsCollectionConfiguration.
        for (value in ContactInformationCollectionMode.entries) {
            if (value.isRequired(billingDetailsCollectionConfiguration)) {
                requireContactInformationIfAllowed(value)
            }
        }

        // Setup the required billing fields section based on the merchant billingDetailsCollectionConfiguration.
        if (billingDetailsCollectionConfiguration.address
            == PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        ) {
            requireBillingAddressIfAllowed()
        }
    }

    fun nonInteractiveHeader(factory: (UiDefinitionFactory.Arguments) -> FormElement): FormElementsBuilder = apply {
        headerFormElements += FormElementDefinition.nonInteractive(factory)
    }

    fun ignoreContactInformationRequirements() = apply {
        requiredContactInformationCollectionModes.clear()
    }

    fun requireContactInformationIfAllowed(type: ContactInformationCollectionMode): FormElementsBuilder = apply {
        if (type.isAllowed(billingDetailsCollectionConfiguration)) {
            requiredContactInformationCollectionModes += type
        }
    }

    fun overrideContactInformationPosition(type: ContactInformationCollectionMode): FormElementsBuilder {
        return overrideContactInformationPosition(
            type = type,
            factory = { type.formElement(it.initialValues) },
        )
    }

    fun overrideContactInformationPosition(
        type: ContactInformationCollectionMode,
        factory: (UiDefinitionFactory.Arguments) -> FormElement,
    ): FormElementsBuilder = apply {
        if (type in requiredContactInformationCollectionModes) {
            overriddenContactInformationCollectionModes += type

            uiFormElements += FormElementDefinition.interactive(factory)
        }
    }

    fun interactiveElement(factory: (UiDefinitionFactory.Arguments) -> FormElement): FormElementsBuilder = apply {
        uiFormElements += FormElementDefinition.interactive(factory)
    }

    fun ignoreBillingAddressRequirements() = apply {
        requireBillingAddressCollection = false
    }

    fun requireBillingAddressIfAllowed(
        availableCountries: Set<String> = this.availableCountries,
    ): FormElementsBuilder = apply {
        if (billingDetailsCollectionConfiguration.address
            != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        ) {
            requireBillingAddressCollection = true

            this.availableCountries = availableCountries
        }
    }

    fun requireCountry(
        allowedCountryCodes: Set<String>,
        initialValue: (UiDefinitionFactory.Arguments) -> String?,
    ): FormElementsBuilder = apply {
        countryRequirement = CountryRequirementDefinition(
            allowedCountryCodes = allowedCountryCodes,
            initialValue = initialValue,
        )
    }

    fun addMandateIfAllowed(
        stringResId: Int,
        args: List<String>,
        identifier: FormFieldId,
        topPadding: Dp,
    ): FormElementsBuilder = apply {
        if (requiresMandate) {
            mandateFooter(stringResId, args, identifier, topPadding)
        }
    }

    fun addMandateIfAllowed(
        mandateText: ResolvableString,
        factory: (UiDefinitionFactory.Arguments) -> FormElement,
    ): FormElementsBuilder = apply {
        if (requiresMandate) {
            nonInteractiveFooter(mandateText, factory)
        }
    }

    fun interactiveMandateIfAllowed(
        factory: (UiDefinitionFactory.Arguments) -> FormElement,
    ): FormElementsBuilder = apply {
        if (requiresMandate) {
            interactiveFooter { listOf(factory(it)) }
        }
    }

    fun mandateFooter(
        stringResId: Int,
        args: List<String>,
        identifier: FormFieldId,
        topPadding: Dp,
    ): FormElementsBuilder = apply {
        val mandate = MandateDefinition(identifier, stringResId, args)
        nonInteractiveFooter(mandate.text) { mandate.createElement(topPadding) }
    }

    fun nonInteractiveFooter(
        mandateText: ResolvableString?,
        factory: (UiDefinitionFactory.Arguments) -> FormElement,
    ): FormElementsBuilder = apply {
        footerFormElements += FormElementDefinition(false, mandateText) { listOf(factory(it)) }
    }

    fun interactiveFooter(
        factory: (UiDefinitionFactory.Arguments) -> List<FormElement>,
    ): FormElementsBuilder = apply {
        footerFormElements += FormElementDefinition(true, null, factory)
    }

    fun build(): FormDefinition {
        return FormDefinition(
            buildList {
                addAll(headerFormElements) // Order headers first.

                for (collectionMode in requiredContactInformationCollectionModes) {
                    if (collectionMode !in overriddenContactInformationCollectionModes) {
                        add(FormElementDefinition.interactive { collectionMode.formElement(it.initialValues) })
                    }
                }

                addAll(uiFormElements)

                addAll(
                    BillingAddressFormElementsBuilder(
                        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                        requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
                        supportsAutomaticTaxBillingAddress = supportsAutomaticTaxBillingAddress,
                        requireBillingAddressCollection = requireBillingAddressCollection,
                        fallbackCountryCodes = availableCountries,
                        countryRequirement = countryRequirement,
                    ).build()
                )

                addAll(footerFormElements) // Order footers last.
            }
        )
    }
}

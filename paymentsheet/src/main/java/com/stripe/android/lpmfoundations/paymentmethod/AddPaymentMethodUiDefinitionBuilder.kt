package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.AddPaymentMethodSelectorUiDefinition
import com.stripe.android.lpmfoundations.AddPaymentMethodSelectorUiDefinitionBuilder
import com.stripe.android.lpmfoundations.AddPaymentMethodUiDefinition
import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.UiElementDefinition
import com.stripe.android.lpmfoundations.uielements.BillingAddressUiElementDefinition
import com.stripe.android.lpmfoundations.uielements.ContactInformationUiElementDefinition
import com.stripe.android.lpmfoundations.uielements.MandateUiElementDefinition
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode

internal class AddPaymentMethodUiDefinitionBuilder(
    private val paymentMethodDefinition: PaymentMethodDefinition,
    private val metadata: PaymentMethodMetadata,
) {
    private lateinit var addPaymentMethodSelectorUiDefinition: AddPaymentMethodSelectorUiDefinition

    private val headerUiElementDefinitions: MutableList<UiElementDefinition> = mutableListOf()
    private val uiElementDefinitions: MutableList<UiElementDefinition> = mutableListOf()
    private val footerUiElementDefinitions: MutableList<UiElementDefinition> = mutableListOf()

    private val requiredContactInformationCollectionModes: MutableSet<ContactInformationCollectionMode> = mutableSetOf()

    var requireBillingAddressCollection: Boolean = false
        private set
    private var availableCountries: Set<String> = CountryUtils.supportedBillingCountries

    init {
        // Setup the required contact information fields based on the merchant billingDetailsCollectionConfiguration.
        for (value in ContactInformationCollectionMode.values()) {
            if (value.isRequired(metadata.billingDetailsCollectionConfiguration)) {
                requireContactInformation(value)
            }
        }

        // Setup the required billing fields section based on the merchant billingDetailsCollectionConfiguration.
        if (metadata.billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full) {
            requireBillingAddress()
        }
    }

    fun selector(builder: AddPaymentMethodSelectorUiDefinitionBuilder.() -> Unit) {
        addPaymentMethodSelectorUiDefinition = AddPaymentMethodSelectorUiDefinitionBuilder().apply {
            // Default the icon image URLs to what the spec returns.
            // TODO(jaynewstrom): Parse the information from the LUXE Spec.
//            val selectorIcon = metadata.sharedDataSpecs.firstOrNull { spec ->
//                spec.type == paymentMethodDefinition.type.code
//            }?.selectorIcon
//            if (selectorIcon != null) {
//                if (selectorIcon.lightThemePng != null) {
//                    lightThemeIconUrl = selectorIcon.lightThemePng
//                }
//                if (selectorIcon.darkThemePng != null) {
//                    darkThemeIconUrl = selectorIcon.darkThemePng
//                }
//            }
        }.also(builder).build()
    }

    fun header(uiElementDefinition: UiElementDefinition) {
        headerUiElementDefinitions += uiElementDefinition
    }

    fun requireContactInformation(type: ContactInformationCollectionMode) {
        if (type.isAllowed(metadata.billingDetailsCollectionConfiguration)) {
            requiredContactInformationCollectionModes += type
        }
    }

    fun element(uiElementDefinition: UiElementDefinition) {
        uiElementDefinitions += uiElementDefinition
    }

    fun requireBillingAddress(
        availableCountries: Set<String> = this.availableCountries,
    ) {
        if (metadata.billingDetailsCollectionConfiguration.address != AddressCollectionMode.Never) {
            requireBillingAddressCollection = true

            this.availableCountries = availableCountries
        }
    }

    fun mandate(text: ResolvableString) {
        footerUiElementDefinitions += MandateUiElementDefinition(text)
    }

    fun footer(uiElementDefinition: UiElementDefinition) {
        footerUiElementDefinitions += uiElementDefinition
    }

    fun build(): AddPaymentMethodUiDefinition {
        val elementDefinitionList: List<UiElementDefinition> = buildList {
            addAll(headerUiElementDefinitions) // Order headers first.

            if (requiredContactInformationCollectionModes.isNotEmpty()) {
                add(ContactInformationUiElementDefinition(requiredContactInformationCollectionModes))
            }

            addAll(uiElementDefinitions)

            if (requireBillingAddressCollection) {
                add(BillingAddressUiElementDefinition(availableCountries))
            }

            addAll(footerUiElementDefinitions) // Order footers last.
        }

        return AddPaymentMethodUiDefinition(
            identifier = paymentMethodDefinition.type.code,
            addPaymentMethodSelectorUiDefinition = addPaymentMethodSelectorUiDefinition,
            uiElementsDefinitions = elementDefinitionList,
        )
    }
}

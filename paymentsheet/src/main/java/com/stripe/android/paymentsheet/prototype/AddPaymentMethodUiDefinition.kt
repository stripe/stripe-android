package com.stripe.android.paymentsheet.prototype

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.prototype.uielement.BillingAddressUiElementDefinition
import com.stripe.android.paymentsheet.prototype.uielement.ContactInformationUiElementDefinition
import com.stripe.android.paymentsheet.prototype.uielement.MandateUiElementDefinition

internal data class AddPaymentMethodUiDefinition(
    val addPaymentMethodSelectorUiDefinition: AddPaymentMethodSelectorUiDefinition,
    val uiElementsDefinitions: List<UiElementDefinition>,
)

internal class AddPaymentMethodUiDefinitionBuilder(
    private val paymentMethodDefinition: PaymentMethodDefinition,
    private val metadata: ParsingMetadata,
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
            if (value.isRequired(metadata.configuration.billingDetailsCollectionConfiguration)) {
                requireContactInformation(value)
            }
        }

        // Setup the required billing fields section based on the merchant billingDetailsCollectionConfiguration.
        if (metadata.configuration.billingDetailsCollectionConfiguration.address == PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full) {
            requireBillingAddress()
        }
    }

    fun selector(builder: AddPaymentMethodSelectorUiDefinitionBuilder.() -> Unit) {
        addPaymentMethodSelectorUiDefinition = AddPaymentMethodSelectorUiDefinitionBuilder().apply {
            // Default the icon image URLs to what the spec returns.
            val selectorIcon = metadata.sharedDataSpecs.firstOrNull { spec ->
                spec.type == paymentMethodDefinition.type.code
            }?.selectorIcon
            if (selectorIcon != null) {
                if (selectorIcon.lightThemePng != null) {
                    lightThemeIconUrl = selectorIcon.lightThemePng
                }
                if (selectorIcon.darkThemePng != null) {
                    darkThemeIconUrl = selectorIcon.darkThemePng
                }
            }
        }.also(builder).build()
    }

    fun header(uiElementDefinition: UiElementDefinition) {
        headerUiElementDefinitions += uiElementDefinition
    }

    fun requireContactInformation(type: ContactInformationCollectionMode) {
        if (type.isAllowed(metadata.configuration.billingDetailsCollectionConfiguration)) {
            requiredContactInformationCollectionModes += type
        }
    }

    fun element(uiElementDefinition: UiElementDefinition) {
        uiElementDefinitions += uiElementDefinition
    }

    fun requireBillingAddress(
        availableCountries: Set<String> = this.availableCountries,
    ) {
        if (metadata.configuration.billingDetailsCollectionConfiguration.address != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never) {
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
            addPaymentMethodSelectorUiDefinition = addPaymentMethodSelectorUiDefinition,
            uiElementsDefinitions = elementDefinitionList,
        )
    }
}

internal enum class ContactInformationCollectionMode {
    Name {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ) = configuration.name
    },
    Phone {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ) = configuration.phone
    },
    Email {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ) = configuration.email
    };

    abstract fun collectionMode(
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration
    ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

    fun isAllowed(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode != PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
    }

    fun isRequired(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode == PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
    }
}

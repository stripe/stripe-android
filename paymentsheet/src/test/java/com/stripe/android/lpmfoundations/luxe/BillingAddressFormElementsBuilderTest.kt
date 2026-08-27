package com.stripe.android.lpmfoundations.luxe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BillingAddressFormElementsBuilderTest {
    @Test
    fun `country requirement creates standalone country`() {
        val countryElement = billingAddressFormElementsBuilder(
            arguments = arguments(),
            countryRequirement = CountryRequirement(
                allowedCountryCodes = setOf("CA"),
                initialValue = "CA",
            ),
        ).build().countryElement()

        assertThat(countryElement.controller.displayItems).containsExactly("🇨🇦 Canada")
        assertThat(countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `no requirement returns empty output`() {
        assertThat(
            billingAddressFormElementsBuilder(arguments = arguments()).build()
        ).isEmpty()
    }

    @Test
    fun `country requirement overrides full address countries and initial country`() {
        val addressElement = billingAddressFormElementsBuilder(
            arguments = arguments(
                initialValues = mapOf(IdentifierSpec.Country to "US"),
            ),
            requireBillingAddressCollection = true,
            fallbackCountryCodes = setOf("US"),
            countryRequirement = CountryRequirement(
                allowedCountryCodes = setOf("BS", "CA"),
                initialValue = "BS",
            ),
        ).build().addressElement()

        assertThat(addressElement.countryElement.controller.displayItems).containsExactly(
            "🇧🇸 Bahamas",
            "🇨🇦 Canada",
        )
        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("BS")
    }

    @Test
    fun `country requirement explicit null overrides full address initial country`() {
        val addressElement = billingAddressFormElementsBuilder(
            arguments = arguments(
                initialValues = mapOf(IdentifierSpec.Country to "US"),
            ),
            requireBillingAddressCollection = true,
            fallbackCountryCodes = setOf("US"),
            countryRequirement = CountryRequirement(
                allowedCountryCodes = linkedSetOf("CA", "DE"),
                initialValue = null,
            ),
        ).build().addressElement()

        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `full address falls back to fallback country codes`() {
        val addressElement = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    allowedCountries = setOf("US"),
                ),
                initialValues = mapOf(IdentifierSpec.Country to "CA"),
            ),
            requireBillingAddressCollection = true,
            fallbackCountryCodes = setOf("CA"),
        ).build().addressElement()

        assertThat(addressElement.countryElement.controller.displayItems).containsExactly("🇨🇦 Canada")
        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `full address uses all supported countries when merchant countries are empty`() {
        val addressElement = billingAddressFormElementsBuilder(
            arguments = arguments(),
            requireBillingAddressCollection = true,
        ).build().addressElement()

        assertThat(addressElement.countryElement.controller.displayItems)
            .hasSize(CountryUtils.supportedBillingCountries.size)
    }

    @Test
    fun `full address uses autocomplete`() {
        val formElements = billingAddressFormElementsBuilder(
            arguments = arguments(
                autocompleteAddressInteractorFactory = {
                    TestAutocompleteAddressInteractor.noOp()
                },
            ),
            requireBillingAddressCollection = true,
        ).build()

        assertThat((formElements.single() as SectionElement).fields.single())
            .isInstanceOf<AutocompleteAddressElement>()
    }

    private fun billingAddressFormElementsBuilder(
        arguments: UiDefinitionFactory.Arguments,
        requireBillingAddressCollection: Boolean = false,
        fallbackCountryCodes: Set<String> =
            arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
        countryRequirement: CountryRequirement? = null,
    ): BillingAddressFormElementsBuilder {
        return BillingAddressFormElementsBuilder(
            arguments = arguments,
            requireBillingAddressCollection = requireBillingAddressCollection,
            fallbackCountryCodes = fallbackCountryCodes,
            countryRequirement = countryRequirement,
        )
    }

    private fun List<FormElement>.addressElement(): AddressElement {
        return (single() as SectionElement).fields.single() as AddressElement
    }

    private fun List<FormElement>.countryElement(): CountryElement {
        return (single() as SectionElement).fields.single() as CountryElement
    }

    private fun arguments(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
        initialValues: Map<IdentifierSpec, String?> = emptyMap(),
        shippingValues: Map<IdentifierSpec, String?>? = emptyMap(),
    ): UiDefinitionFactory.Arguments {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return UiDefinitionFactory.Arguments(
            initialValues = initialValues,
            initialLinkUserInput = null,
            shippingValues = shippingValues,
            saveForFutureUseInitialValue = false,
            merchantName = "Example Inc.",
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            requiresBillingAddressForAutomaticTax = false,
            requiresMandate = false,
            linkConfigurationCoordinator = null,
            onLinkInlineSignupStateChanged = { throw AssertionError("Not implemented") },
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter,
            setAsDefaultMatchesSaveForFutureUse = false,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            linkInlineHandler = null,
        )
    }
}

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
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BillingAddressFormElementsBuilderTest {
    @Test
    fun `full address takes precedence over automatic tax`() {
        val formElements = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
                requiresBillingAddressForAutomaticTax = true,
            ),
            requireBillingAddressCollection = true,
        ).build()

        val addressField = formElements.addressElement()
        assertThat(addressField).isNotInstanceOf(BillingAddressElement::class.java)
    }

    @Test
    fun `method-required address takes precedence over automatic tax`() {
        val formElements = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
            ),
            requireBillingAddressCollection = true,
        ).build()

        val addressField = formElements.addressElement()
        assertThat(addressField).isNotInstanceOf(BillingAddressElement::class.java)
    }

    @Test
    fun `automatic tax collects billing address`() {
        val billingAddressElement = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(
                    allowedCountries = setOf("US"),
                ),
                requiresBillingAddressForAutomaticTax = true,
                initialValues = mapOf(IdentifierSpec.Country to "US"),
            ),
        ).build().billingAddressElement()

        assertThat(billingAddressElement.hiddenIdentifiers.value).contains(IdentifierSpec.Line2)
        assertThat(billingAddressElement.hiddenIdentifiers.value).containsNoneOf(
            IdentifierSpec.Line1,
            IdentifierSpec.City,
            IdentifierSpec.State,
            IdentifierSpec.PostalCode,
        )
    }

    @Test
    fun `unsupported automatic tax returns empty output`() {
        val formElements = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
            ),
            supportsAutomaticTaxBillingAddress = false,
        ).build()

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `tax-disabled automatic collection preserves standalone country`() {
        val formElements = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = false,
            ),
            countryRequirement = CountryRequirement(
                allowedCountryCodes = setOf("US"),
                initialValue = "US",
            ),
        ).build()

        assertThat(formElements.countryElement().controller.rawFieldValue.value).isEqualTo("US")
    }

    @Test
    fun `never collection returns empty output`() {
        val formElements = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
                requiresBillingAddressForAutomaticTax = true,
            ),
        ).build()

        assertThat(formElements).isEmpty()
    }

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
    fun `country requirement overrides automatic tax countries and initial country`() {
        val countryElement = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(
                    allowedCountries = setOf("US"),
                ),
                requiresBillingAddressForAutomaticTax = true,
                initialValues = mapOf(IdentifierSpec.Country to "US"),
            ),
            countryRequirement = CountryRequirement(
                allowedCountryCodes = setOf("DE", "FR"),
                initialValue = "FR",
            ),
        ).build().billingAddressElement().countryElement

        assertThat(countryElement.controller.displayItems).containsExactly(
            "🇫🇷 France",
            "🇩🇪 Germany",
        ).inOrder()
        assertThat(countryElement.controller.rawFieldValue.value).isEqualTo("FR")
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
    fun `automatic tax falls back to merchant countries`() {
        val countryElement = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(
                    allowedCountries = setOf("US", "CA"),
                ),
                requiresBillingAddressForAutomaticTax = true,
                initialValues = mapOf(IdentifierSpec.Country to "CA"),
            ),
            fallbackCountryCodes = setOf("BS"),
        ).build().billingAddressElement().countryElement

        assertThat(countryElement.controller.displayItems).containsExactly(
            "🇺🇸 United States",
            "🇨🇦 Canada",
        )
        assertThat(countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `automatic tax preserves same as shipping`() {
        val formElements = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
                shippingValues = mapOf(IdentifierSpec.SameAsShipping to "true"),
            ),
        ).build()

        val sameAsShippingElement = formElements.last() as SameAsShippingElement
        assertThat(sameAsShippingElement.controller.value.value).isTrue()
    }

    @Test
    fun `automatic tax does not use autocomplete`() {
        val billingAddressElement = billingAddressFormElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
                autocompleteAddressInteractorFactory = {
                    TestAutocompleteAddressInteractor.noOp()
                },
            ),
        ).build().billingAddressElement()

        assertThat(billingAddressElement.addressElement).isInstanceOf<AddressElement>()
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
        supportsAutomaticTaxBillingAddress: Boolean = true,
        requireBillingAddressCollection: Boolean = false,
        fallbackCountryCodes: Set<String> =
            arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
        countryRequirement: CountryRequirement? = null,
    ): BillingAddressFormElementsBuilder {
        return BillingAddressFormElementsBuilder(
            arguments = arguments,
            supportsAutomaticTaxBillingAddress = supportsAutomaticTaxBillingAddress,
            requireBillingAddressCollection = requireBillingAddressCollection,
            fallbackCountryCodes = fallbackCountryCodes,
            countryRequirement = countryRequirement,
        )
    }

    private fun List<FormElement>.addressElement(): AddressElement {
        return (single() as SectionElement).fields.single() as AddressElement
    }

    private fun List<FormElement>.billingAddressElement(): BillingAddressElement {
        return filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()
            .single()
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
        requiresBillingAddressForAutomaticTax: Boolean = false,
    ): UiDefinitionFactory.Arguments {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return UiDefinitionFactory.Arguments(
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            initialValues = initialValues,
            initialLinkUserInput = null,
            shippingValues = shippingValues,
            saveForFutureUseInitialValue = false,
            merchantName = "Example Inc.",
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
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

    private fun automaticAddressConfiguration(
        allowedCountries: Set<String> = emptySet(),
    ): PaymentSheet.BillingDetailsCollectionConfiguration {
        return PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            allowedCountries = allowedCountries,
        )
    }
}

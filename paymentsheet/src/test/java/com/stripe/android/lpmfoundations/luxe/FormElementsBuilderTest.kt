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
import com.stripe.android.ui.core.elements.EmptyFormElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormElementsBuilderTest {
    @Test
    fun `build returns an emptyList`() {
        assertThat(formElementsBuilder(arguments()).build()).isEmpty()
    }

    @Test
    fun `build returns all required billing fields`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )
        val formElements = formElementsBuilder(arguments).build()
        assertThat(formElements).hasSize(4)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[name]_section")
        assertThat(formElements[1].identifier.v1).isEqualTo("billing_details[phone]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[3].identifier.v1).isEqualTo("billing_details[address]_section")
    }

    @Test
    fun `build orders fields correctly`() {
        val formElements = formElementsBuilder(arguments())
            .element(EmptyFormElement(identifier = IdentifierSpec(v1 = "element")))
            .header(EmptyFormElement(identifier = IdentifierSpec(v1 = "header")))
            .footer(EmptyFormElement(identifier = IdentifierSpec(v1 = "footer")))
            .requireBillingAddressIfAllowed()
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .build()
        assertThat(formElements).hasSize(5)
        assertThat(formElements[0].identifier.v1).isEqualTo("header")
        assertThat(formElements[1].identifier.v1).isEqualTo("billing_details[name]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("element")
        assertThat(formElements[3].identifier.v1).isEqualTo("billing_details[address]_section")
        assertThat(formElements[4].identifier.v1).isEqualTo("footer")
    }

    @Test
    fun `build returns no billing fields if specified as never`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
        )
        val formElements = formElementsBuilder(arguments)
            .requireBillingAddressIfAllowed()
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Phone)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .build()
        assertThat(formElements).isEmpty()
    }

    @Test
    fun `build returns fields in the order they were added`() {
        val formElements = formElementsBuilder(arguments())
            .element(EmptyFormElement(identifier = IdentifierSpec(v1 = "element1")))
            .element(EmptyFormElement(identifier = IdentifierSpec(v1 = "element2")))
            .footer(EmptyFormElement(identifier = IdentifierSpec(v1 = "footer1")))
            .footer(EmptyFormElement(identifier = IdentifierSpec(v1 = "footer2")))
            .header(EmptyFormElement(identifier = IdentifierSpec(v1 = "header1")))
            .header(EmptyFormElement(identifier = IdentifierSpec(v1 = "header2")))
            .build()
        assertThat(formElements).hasSize(6)
        assertThat(formElements[0].identifier.v1).isEqualTo("header1")
        assertThat(formElements[1].identifier.v1).isEqualTo("header2")
        assertThat(formElements[2].identifier.v1).isEqualTo("element1")
        assertThat(formElements[3].identifier.v1).isEqualTo("element2")
        assertThat(formElements[4].identifier.v1).isEqualTo("footer1")
        assertThat(formElements[5].identifier.v1).isEqualTo("footer2")
    }

    @Test
    fun `build returns no billing address fields if ignored`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )
        val formElements = formElementsBuilder(arguments)
            .element(EmptyFormElement(identifier = IdentifierSpec(v1 = "element")))
            .ignoreBillingAddressRequirements()
            .build()
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("element")
    }

    @Test
    fun `build returns no contact information fields if ignored`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        )
        val formElements = formElementsBuilder(arguments)
            .element(EmptyFormElement(identifier = IdentifierSpec(v1 = "element")))
            .ignoreContactInformationRequirements()
            .build()
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("element")
    }

    @Test
    fun `build should return AutocompleteAddressElement if factory is provided`() = runTest {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
            autocompleteAddressInteractorFactory = {
                TestAutocompleteAddressInteractor.noOp()
            }
        )

        val formElements = formElementsBuilder(arguments).build()

        assertThat(formElements).hasSize(1)
        assertThat(formElements.firstOrNull()).isInstanceOf<SectionElement>()

        val sectionElement = formElements.first() as SectionElement

        assertThat(sectionElement.fields.size).isEqualTo(1)
        assertThat(sectionElement.fields.firstOrNull()).isInstanceOf<AutocompleteAddressElement>()
    }

    @Test
    fun `build contains all supported billing countries when allowed countries is empty`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                allowedCountries = emptySet()
            )
        )
        val formElements = formElementsBuilder(arguments).build()

        assertThat(formElements).hasSize(1)
        assertThat(formElements.firstOrNull()).isInstanceOf<SectionElement>()

        val sectionElement = formElements.first() as SectionElement
        val sectionFields = sectionElement.fields

        assertThat(sectionFields.size).isEqualTo(1)
        assertThat(sectionFields.firstOrNull()).isInstanceOf<AddressElement>()

        val addressElement = sectionFields.first() as AddressElement

        assertThat(addressElement.countryElement.controller.displayItems)
            .hasSize(CountryUtils.supportedBillingCountries.size)
    }

    @Test
    fun `build contains only countries provided through billing configuration`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                allowedCountries = setOf("US", "CA")
            )
        )
        val formElements = formElementsBuilder(arguments).build()

        assertThat(formElements).hasSize(1)
        assertThat(formElements.firstOrNull()).isInstanceOf<SectionElement>()

        val sectionElement = formElements.first() as SectionElement
        val sectionFields = sectionElement.fields

        assertThat(sectionFields.size).isEqualTo(1)
        assertThat(sectionFields.firstOrNull()).isInstanceOf<AddressElement>()

        val addressElement = sectionFields.first() as AddressElement

        assertThat(addressElement.countryElement.controller.displayItems).containsExactly(
            "\uD83C\uDDFA\uD83C\uDDF8 United States",
            "\uD83C\uDDE8\uD83C\uDDE6 Canada"
        )
    }

    @Test
    fun `build orders all overridden contact information fields based on function order`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                allowedCountries = setOf("US", "CA")
            )
        )
        val formElements = formElementsBuilder(arguments)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .build()

        assertThat(formElements).hasSize(3)

        val phoneSection = getSection(formElements, "billing_details[phone]_section", 0)
        phoneSection.assertSingleElementInSection<PhoneNumberElement>()

        val nameSection = getSection(formElements, "billing_details[name]_section", 1)
        nameSection.assertSingleElementInSection<SimpleTextElement>()

        val emailSection = getSection(formElements, "billing_details[email]_section", 2)
        emailSection.assertSingleElementInSection<EmailElement>()
    }

    @Test
    fun `build orders only overridden contact information fields based on function order`() {
        val arguments = arguments(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                allowedCountries = setOf("US", "CA")
            )
        )
        val formElements = formElementsBuilder(arguments)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .build()

        assertThat(formElements).hasSize(3)

        val emailSection = getSection(formElements, "billing_details[email]_section", 0)
        emailSection.assertSingleElementInSection<EmailElement>()

        val phoneSection = getSection(formElements, "billing_details[phone]_section", 1)
        phoneSection.assertSingleElementInSection<PhoneNumberElement>()

        val nameSection = getSection(formElements, "billing_details[name]_section", 2)
        nameSection.assertSingleElementInSection<SimpleTextElement>()
    }

    @Test
    fun `build promotes country-only source for automatic tax`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
                initialValues = mapOf(IdentifierSpec.Country to "DE"),
            ),
        ).countryOnly(
            allowedCountryCodes = setOf("DE", "FR"),
        ).build()

        val billingAddressElement = (formElements.single() as SectionElement)
            .fields.single() as BillingAddressElement
        assertThat(billingAddressElement.countryElement.controller.rawFieldValue.value).isEqualTo("DE")
        assertThat(billingAddressElement.countryElement.controller.displayItems).containsExactly(
            "🇫🇷 France",
            "🇩🇪 Germany",
        ).inOrder()
    }

    @Test
    fun `build keeps country-only source without card AVS fields when automatic tax is disabled`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = false,
                initialValues = mapOf(IdentifierSpec.Country to "GB"),
            ),
        ).countryOnly(
            allowedCountryCodes = setOf("GB"),
        ).build()

        val countryElement = (formElements.single() as SectionElement).fields.single()
        assertThat(countryElement).isInstanceOf<CountryElement>()
        assertThat(formElements.filterIsInstance<SameAsShippingElement>()).isEmpty()
    }

    @Test
    fun `build replaces country-only source with a full address`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
        ).countryOnly(
            allowedCountryCodes = setOf("DE", "FR"),
        ).build()

        val addressElement = (formElements.single() as SectionElement).fields.single() as AddressElement
        assertThat(addressElement.countryElement.controller.displayItems).containsExactly(
            "🇫🇷 France",
            "🇩🇪 Germany",
        ).inOrder()
    }

    @Test
    fun `build inserts resolved country-only source at its declaration position`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
            ),
        ).element(
            EmptyFormElement(identifier = IdentifierSpec.Generic("before")),
        ).countryOnly(
            allowedCountryCodes = setOf("US"),
        ).element(
            EmptyFormElement(identifier = IdentifierSpec.Generic("after")),
        ).build()

        assertThat(formElements.map { it.identifier.v1 }).containsExactly(
            "before",
            "billing_details[address]_section",
            "after",
        ).inOrder()
    }

    @Test
    fun `country-only source cannot follow a normal address source`() {
        val error = runCatching {
            formElementsBuilder(arguments())
                .requireBillingAddressIfAllowed()
                .countryOnly(allowedCountryCodes = setOf("US"))
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `normal address source cannot follow a country-only source`() {
        val error = runCatching {
            formElementsBuilder(arguments())
                .countryOnly(allowedCountryCodes = setOf("US"))
                .requireBillingAddressIfAllowed()
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `build adds automatic tax address before footer`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
            ),
        ).element(
            EmptyFormElement(identifier = IdentifierSpec.Generic("element")),
        ).footer(
            EmptyFormElement(identifier = IdentifierSpec.Generic("footer")),
        ).build()

        assertThat(formElements.map { it.identifier.v1 }).containsExactly(
            "element",
            "billing_details[address]_section",
            "footer",
        ).inOrder()
    }

    @Test
    fun `build preserves required normal address for automatic tax`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
            ),
        ).requireBillingAddressIfAllowed().build()

        val addressField = (formElements.single() as SectionElement).fields.single()
        assertThat(addressField).isInstanceOf<AddressElement>()
    }

    @Test
    fun `build preserves same as shipping state for automatic tax address`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
                shippingValues = mapOf(IdentifierSpec.SameAsShipping to "true"),
            ),
        ).countryOnly(
            allowedCountryCodes = setOf("US"),
        ).build()

        val sameAsShippingElement = formElements[1] as SameAsShippingElement
        assertThat(sameAsShippingElement.controller.value.value).isTrue()
    }

    private inline fun <reified T : SectionFieldElement> SectionElement.assertSingleElementInSection() {
        assertThat(fields).hasSize(1)
        assertThat(fields[0]).isInstanceOf<T>()
    }

    private fun getSection(
        formElements: List<FormElement>,
        identifierName: String,
        position: Int,
    ): SectionElement {
        assertThat(formElements[position].identifier.v1).isEqualTo(identifierName)
        assertThat(formElements[position]).isInstanceOf<SectionElement>()

        return formElements[position] as SectionElement
    }

    private fun arguments(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
        requiresBillingAddressForAutomaticTax: Boolean = false,
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

    private fun formElementsBuilder(
        arguments: UiDefinitionFactory.Arguments,
    ): FormElementsBuilder {
        return FormElementsBuilder(
            arguments = arguments,
            supportsAutomaticTaxBillingAddress = true,
        )
    }

    private fun automaticAddressConfiguration(): PaymentSheet.BillingDetailsCollectionConfiguration {
        return PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        )
    }
}

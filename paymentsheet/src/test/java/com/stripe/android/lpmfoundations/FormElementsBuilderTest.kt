package com.stripe.android.lpmfoundations

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            .element(testElement(IdentifierSpec(v1 = "element")))
            .header(testElement(IdentifierSpec(v1 = "header")))
            .footer(testElement(IdentifierSpec(v1 = "footer")))
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
    fun `build orders country requirement after ordinary elements and before footers`() {
        val formElements = formElementsBuilder(arguments())
            .element(testElement(IdentifierSpec(v1 = "element")))
            .footer(testElement(IdentifierSpec(v1 = "footer")))
            .requireCountry(
                allowedCountryCodes = setOf("US", "CA"),
                initialValue = "US",
            )
            .build()

        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("element")
        assertThat(formElements[1].identifier.v1).isEqualTo("billing_details[address][country]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("footer")
    }

    @Test
    fun `automatic tax adds billing address after ordinary elements and before footers`() {
        val formElements = formElementsBuilder(
            arguments = arguments(
                billingDetailsCollectionConfiguration = automaticAddressConfiguration(),
                requiresBillingAddressForAutomaticTax = true,
            ),
        )
            .element(testElement(IdentifierSpec.Generic("element")))
            .footer(testElement(IdentifierSpec.Generic("footer")))
            .build()

        assertThat(formElements.map { it.identifier.v1 }).containsExactly(
            "element",
            "billing_details[address]_section",
            "footer",
        ).inOrder()
        formElements.billingAddressElement()
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
            .element(testElement(IdentifierSpec(v1 = "element1")))
            .element(testElement(IdentifierSpec(v1 = "element2")))
            .footer(testElement(IdentifierSpec(v1 = "footer1")))
            .footer(testElement(IdentifierSpec(v1 = "footer2")))
            .header(testElement(IdentifierSpec(v1 = "header1")))
            .header(testElement(IdentifierSpec(v1 = "header2")))
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
            .element(testElement(IdentifierSpec(v1 = "element")))
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
            .element(testElement(IdentifierSpec(v1 = "element")))
            .ignoreContactInformationRequirements()
            .build()
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("element")
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

    private inline fun <reified T : SectionFieldElement> SectionElement.assertSingleElementInSection() {
        assertThat(fields).hasSize(1)
        assertThat(fields[0]).isInstanceOf<T>()
    }

    private fun List<FormElement>.billingAddressElement(): BillingAddressElement {
        return filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()
            .single()
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

    private fun testElement(identifier: IdentifierSpec): FormElement {
        return StaticTextElement(
            identifier = identifier,
            text = resolvableString("Test element"),
        )
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

    private fun formElementsBuilder(
        arguments: UiDefinitionFactory.Arguments,
        supportsAutomaticTaxBillingAddress: Boolean = true,
    ): FormElementsBuilder {
        return FormElementsBuilder(
            arguments = arguments,
            supportsAutomaticTaxBillingAddress = supportsAutomaticTaxBillingAddress,
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

internal fun List<FormElement>.countryElements(): List<CountryElement> {
    return filterIsInstance<SectionElement>().flatMap { section ->
        section.fields.mapNotNull { field ->
            when (field) {
                is CountryElement -> field
                is BillingAddressElement -> field.countryElement
                is AddressElement -> field.countryElement
                else -> null
            }
        }
    }
}

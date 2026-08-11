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
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.filterOutHiddenIdentifiers
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
            .requireBillingAddressIfAllowed(availableCountries = emptySet())
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
            .requireBillingAddressIfAllowed(availableCountries = emptySet())
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
            .suppressBillingAddressRequirements()
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
        val billingAddressElement = sectionElement.fields.single() as BillingAddressElement
        assertThat(billingAddressElement.addressElement).isInstanceOf<AutocompleteAddressElement>()
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
        val addressElement = sectionFields.single() as BillingAddressElement

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
        val addressElement = sectionFields.single() as BillingAddressElement

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
    fun `automatic with billing tax adds tax minimum for no source`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Automatic,
                    requiresBillingAddressForAutomaticTax = true,
                    initialValues = mapOf(IdentifierSpec.Country to "US"),
                )
            )
        )

        assertThat(element.shownIdentifierParamPaths()).containsExactly(
            IdentifierSpec.Country.v1,
            IdentifierSpec.Line1.v1,
            IdentifierSpec.City.v1,
            IdentifierSpec.State.v1,
            IdentifierSpec.PostalCode.v1,
        )
    }

    @Test
    fun `country-only remains country-only for never`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Never,
                    initialValues = mapOf(IdentifierSpec.Country to "DE"),
                )
            ).countryOnly(
                allowedCountryCodes = setOf("DE", "FR"),
                initialValue = "DE",
            )
        )

        assertThat(element.shownIdentifierParamPaths()).containsExactly(IdentifierSpec.Country.v1)
        assertThat(element.countryElement.controller.displayItems).containsExactly(
            "🇫🇷 France",
            "🇩🇪 Germany",
        ).inOrder()
    }

    @Test
    fun `country-only remains country-only for automatic without tax`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Automatic,
                )
            ).countryOnly(
                allowedCountryCodes = setOf("US"),
                initialValue = "US",
            )
        )

        assertThat(element.shownIdentifierParamPaths()).containsExactly(IdentifierSpec.Country.v1)
    }

    @Test
    fun `country-only uses its countries for automatic tax minimum`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Automatic,
                    requiresBillingAddressForAutomaticTax = true,
                )
            ).countryOnly(
                allowedCountryCodes = setOf("CA"),
                initialValue = "CA",
            )
        )

        assertThat(element.countryElement.controller.displayItems).containsExactly("🇨🇦 Canada")
        assertThat(element.shownAddressFields()).containsExactly(IdentifierSpec.PostalCode)
    }

    @Test
    fun `country-only becomes full for full collection`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Full,
                )
            ).countryOnly(
                allowedCountryCodes = setOf("US"),
                initialValue = "US",
            )
        )

        assertThat(element.hiddenIdentifiers.value).isEmpty()
    }

    @Test
    fun `full-if-allowed is absent for never`() {
        val formElements = formElementsBuilder(
            arguments(
                addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
        ).requireBillingAddressIfAllowed(availableCountries = setOf("BR")).build()

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `full-if-allowed is full for automatic`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Automatic,
                )
            ).requireBillingAddressIfAllowed(availableCountries = setOf("BR"))
        )

        assertThat(element.hiddenIdentifiers.value).isEmpty()
        assertThat(element.countryElement.controller.displayItems).containsExactly("🇧🇷 Brazil")
    }

    @Test
    fun `suppression remains absent for automatic without tax`() {
        val formElements = formElementsBuilder(
            arguments(
                addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                    .Automatic,
            )
        ).suppressBillingAddressRequirements().build()

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `billing tax overrides suppression`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Automatic,
                    requiresBillingAddressForAutomaticTax = true,
                    initialValues = mapOf(IdentifierSpec.Country to "GB"),
                )
            ).suppressBillingAddressRequirements()
        )

        assertThat(element.shownAddressFields()).containsExactly(IdentifierSpec.PostalCode)
    }

    @Test
    fun `suppression remains absent for full`() {
        val formElements = formElementsBuilder(
            arguments(
                addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        ).suppressBillingAddressRequirements().build()

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `billing tax overrides suppression for full`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode =
                        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    requiresBillingAddressForAutomaticTax = true,
                    initialValues = mapOf(IdentifierSpec.Country to "US"),
                )
            ).suppressBillingAddressRequirements()
        )

        assertThat(element.shownAddressFields()).containsExactly(
            IdentifierSpec.Line1,
            IdentifierSpec.City,
            IdentifierSpec.State,
            IdentifierSpec.PostalCode,
        )
    }

    @Test
    fun `external factory eligibility disables tax minimum`() {
        val formElements = FormElementsBuilder(
            arguments = arguments(
                addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                    .Automatic,
                requiresBillingAddressForAutomaticTax = true,
            ),
            allowsBillingAddressForAutomaticTax = false,
        ).build()

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `tax minimum exposes same as shipping without autocomplete`() = runTest {
        val formElements = formElementsBuilder(
            arguments(
                addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                    .Automatic,
                requiresBillingAddressForAutomaticTax = true,
                initialValues = mapOf(IdentifierSpec.Country to "US"),
                shippingValues = mapOf(IdentifierSpec.SameAsShipping to "true"),
                autocompleteAddressInteractorFactory = { TestAutocompleteAddressInteractor.noOp() },
            )
        ).build()

        val element = billingAddressElement(formElements)
        assertThat(element.addressElement).isNotInstanceOf(AutocompleteAddressElement::class.java)
        assertThat(formElements.filterIsInstance<SameAsShippingElement>()).hasSize(1)
    }

    @Test
    fun `tax minimum fields match supported countries`() {
        val element = billingAddressElement(
            builder = formElementsBuilder(
                arguments(
                    addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
                        .Automatic,
                    requiresBillingAddressForAutomaticTax = true,
                )
            )
        )

        val expectedFieldsByCountry = mapOf(
            "US" to setOf(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.State, IdentifierSpec.PostalCode),
            "PR" to setOf(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.PostalCode),
            "CA" to setOf(IdentifierSpec.PostalCode),
            "GB" to setOf(IdentifierSpec.PostalCode),
            "IN" to setOf(IdentifierSpec.PostalCode),
            "FR" to emptySet(),
        )

        expectedFieldsByCountry.forEach { (countryCode, expectedFields) ->
            element.countryElement.controller.onRawValueChange(countryCode)
            assertThat(element.shownAddressFields()).isEqualTo(expectedFields)
        }
    }

    private inline fun <reified T : SectionFieldElement> SectionElement.assertSingleElementInSection() {
        assertThat(fields).hasSize(1)
        assertThat(fields[0]).isInstanceOf<T>()
    }

    private fun formElementsBuilder(
        arguments: UiDefinitionFactory.Arguments,
    ): FormElementsBuilder {
        return FormElementsBuilder(
            arguments = arguments,
            allowsBillingAddressForAutomaticTax = true,
        )
    }

    private fun billingAddressElement(builder: FormElementsBuilder): BillingAddressElement {
        return billingAddressElement(builder.build())
    }

    private fun billingAddressElement(formElements: List<FormElement>): BillingAddressElement {
        return formElements.filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<BillingAddressElement>()
            .single()
    }

    private fun BillingAddressElement.shownIdentifierParamPaths(): List<String> {
        return addressController.value.fieldsFlowable.value
            .filterOutHiddenIdentifiers(hiddenIdentifiers.value)
            .flatMap { field ->
                when (field) {
                    is RowElement -> field.fields.map { it.identifier.v1 }
                    else -> listOf(field.identifier.v1)
                }
            }
    }

    private fun BillingAddressElement.shownAddressFields(): Set<IdentifierSpec> {
        val allAddressFields = FieldType.entries
            .filterNot { it == FieldType.Name }
            .map { it.identifierSpec }
            .toSet()
        return allAddressFields - hiddenIdentifiers.value
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
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode? = null,
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
            billingDetailsCollectionConfiguration = addressCollectionMode?.let {
                PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = billingDetailsCollectionConfiguration.name,
                    phone = billingDetailsCollectionConfiguration.phone,
                    email = billingDetailsCollectionConfiguration.email,
                    address = it,
                    attachDefaultsToPaymentMethod =
                        billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod,
                    allowedCountries = billingDetailsCollectionConfiguration.allowedBillingCountries,
                )
            } ?: billingDetailsCollectionConfiguration,
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
}

package com.stripe.android.lpmfoundations.luxe

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.Capitalization
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.KeyboardType
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.elements.TranslationId
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR
import com.stripe.android.core.R as CoreR

@RunWith(RobolectricTestRunner::class)
internal class TransformSpecToElementsTest {

    private val nameSection = NameSpec()

    private val emailSection = EmailSpec()

    private lateinit var transformSpecToElements: TransformSpecToElements

    @Before
    fun beforeTest() {
        transformSpecToElements = TransformSpecToElementsFactory.create()
    }

    @Test
    fun `Adding a country section sets up the section and country elements correctly`() =
        runBlocking {
            val countrySection = CountrySpec(allowedCountryCodes = setOf("AT"))
            val formElement = transformSpecToElements.transform(
                listOf(countrySection),
            )

            val countrySectionElement = formElement.first() as SectionElement
            val countryElement = countrySectionElement.fields[0] as CountryElement

            assertThat(countryElement.controller.displayItems).hasSize(1)
            assertThat(countryElement.controller.displayItems[0]).isEqualTo("🇦🇹 Austria")

            // Verify the correct config is setup for the controller
            assertThat(countryElement.controller.label.first()).isEqualTo(CountryConfig().label)

            assertThat(countrySectionElement.identifier.v1).isEqualTo("billing_details[address][country]_section")

            assertThat(countryElement.identifier.v1).isEqualTo("billing_details[address][country]")
        }

    @Test
    fun `Adding a ideal bank section sets up the section and country elements correctly`() =
        runBlocking {
            val idealSection = IDEAL_BANK_CONFIG
            val formElement = transformSpecToElements.transform(
                listOf(idealSection),
            )

            val idealSectionElement = formElement.first() as SectionElement
            val idealElement = idealSectionElement.fields[0] as SimpleDropdownElement

            // Verify the correct config is setup for the controller
            assertThat(idealElement.controller.label.first()).isEqualTo(R.string.stripe_ideal_bank)

            assertThat(idealSectionElement.identifier.v1).isEqualTo("ideal[bank]_section")

            assertThat(idealElement.identifier.v1).isEqualTo("ideal[bank]")
        }

    @Test
    fun `Add a name section spec sets up the name element correctly`() = runBlocking {
        val formElement = transformSpecToElements.transform(
            listOf(nameSection),
        )

        val nameElement = (formElement.first() as SectionElement)
            .fields[0] as SimpleTextElement

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label.first()).isEqualTo(NameConfig().label)
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")

        assertThat(nameElement.controller.capitalization).isEqualTo(
            KeyboardCapitalization.Words
        )
        assertThat(nameElement.controller.keyboardType).isEqualTo(
            androidx.compose.ui.text.input.KeyboardType.Text
        )
    }

    @Test
    fun `Add a simple text section spec sets up the text element correctly`() = runBlocking {
        val formElement = transformSpecToElements.transform(
            listOf(
                SimpleTextSpec(
                    IdentifierSpec.Generic("simple"),
                    TranslationId.AddressName.resourceId,
                    showOptionalLabel = true,
                    keyboardType = KeyboardType.Text,
                    capitalization = Capitalization.Words
                )
            ),
        )

        val nameElement = (formElement.first() as SectionElement).fields[0]
            as SimpleTextElement

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label.first()).isEqualTo(CoreR.string.stripe_address_label_full_name)
        assertThat(nameElement.identifier.v1).isEqualTo("simple")
        assertThat(nameElement.controller.showOptionalLabel).isTrue()
    }

    @Test
    fun `Add a email section spec sets up the email element correctly`() = runBlocking {
        val formElement = transformSpecToElements.transform(
            listOf(emailSection),
        )

        val emailSectionElement = formElement.first() as SectionElement
        val emailElement = emailSectionElement.fields[0] as EmailElement

        // Verify the correct config is setup for the controller
        assertThat(emailElement.controller.label.first()).isEqualTo(EmailConfig().label)
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")
    }

    @Test
    fun `Add a static text section spec setup of the static element correctly`() {
        val staticText = StaticTextSpec(
            IdentifierSpec.Generic("mandate"),
            stringResId = R.string.stripe_sepa_mandate
        )
        val formElement = transformSpecToElements.transform(
            listOf(staticText),
        )

        val staticTextElement = formElement.first() as StaticTextElement

        assertThat(staticTextElement.controller).isNull()
        assertThat(staticTextElement.stringResId).isEqualTo(staticText.stringResId)
        assertThat(staticTextElement.identifier).isEqualTo(staticText.apiPath)
    }

    @Test
    fun `Add a phone section spec sets up the phone element correctly`() = runBlocking {
        val phoneSpec = PhoneSpec()
        val formElement = transformSpecToElements.transform(
            listOf(phoneSpec),
        )

        val phoneSectionElement = formElement.first() as SectionElement
        val phoneElement = phoneSectionElement.fields[0] as PhoneNumberElement

        // Verify the correct config is setup for the controller
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `Address placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.BillingAddress,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            )
        ).transform(
            listOf(placeholderSpec),
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val addressElement = sectionElement.fields.first() as AddressElement
        assertThat(addressElement.identifier.v1).isEqualTo("billing_details[address]")
    }

    @Test
    fun `Phone placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.Phone,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        ).transform(
            listOf(placeholderSpec),
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val phoneNumberElement = sectionElement.fields.first() as PhoneNumberElement
        assertThat(phoneNumberElement.identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `Name placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.Name,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        ).transform(
            listOf(placeholderSpec),
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val nameElement = sectionElement.fields.first() as SimpleTextElement
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")
    }

    @Test
    fun `Email placeholders get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.Email,
        )
        val formElement = TransformSpecToElementsFactory.create(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        ).transform(
            listOf(placeholderSpec),
        )

        assertThat(formElement).hasSize(1)
        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(1)
        val emailElement = sectionElement.fields.first() as EmailElement
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")
    }

    @Test
    fun `SepaMandateSpec required get transformed to correct fields`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.SepaMandate,
        )
        val formElement = TransformSpecToElementsFactory.create(
            requiresMandate = true
        ).transform(
            listOf(placeholderSpec),
        )

        assertThat(formElement).hasSize(1)
        val mandateTextElement = formElement.first() as MandateTextElement
        assertThat(mandateTextElement.identifier.v1).isEqualTo("sepa_mandate")
    }

    @Test
    fun `SepaMandateSpec when not required get transformed to an empty list`() {
        val placeholderSpec = PlaceholderSpec(
            apiPath = IdentifierSpec.Generic("foobar"),
            field = PlaceholderSpec.PlaceholderField.SepaMandate,
        )
        val formElement = TransformSpecToElementsFactory.create(
            requiresMandate = false
        ).transform(
            listOf(placeholderSpec),
        )

        assertThat(formElement).isEmpty()
    }

    companion object {
        val IDEAL_BANK_CONFIG = DropdownSpec(
            IdentifierSpec.Generic("ideal[bank]"),
            TranslationId.IdealBank,
            listOf(
                DropdownItemSpec(
                    apiValue = "abn_amro",
                    displayText = "ABN Amro"
                ),
                DropdownItemSpec(
                    apiValue = "asn_bank",
                    displayText = "ASN Bank"
                ),
                DropdownItemSpec(
                    apiValue = "bunq",
                    displayText = "bunq B.V.‎"
                ),
                DropdownItemSpec(
                    apiValue = "handelsbanken",
                    displayText = "Handelsbanken"
                ),
                DropdownItemSpec(
                    apiValue = "ing",
                    displayText = "ING Bank"
                ),
                DropdownItemSpec(
                    apiValue = "knab",
                    displayText = "Knab"
                ),
                DropdownItemSpec(
                    apiValue = "rabobank",
                    displayText = "Rabobank"
                ),
                DropdownItemSpec(
                    apiValue = "regiobank",
                    displayText = "RegioBank"
                ),
                DropdownItemSpec(
                    apiValue = "revolut",
                    displayText = "Revolut"
                ),
                DropdownItemSpec(
                    apiValue = "sns_bank",
                    displayText = "SNS Bank"
                ),
                DropdownItemSpec(
                    apiValue = "triodos_bank",
                    displayText = "Triodos Bank"
                ),
                DropdownItemSpec(
                    apiValue = "van_lanschot",
                    displayText = "Van Lanschot"
                ),
                DropdownItemSpec(
                    apiValue = null, // HIGHLIGHT
                    displayText = "Other"
                )
            )
        )
    }
}

private object TransformSpecToElementsFactory {
    fun create(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        requiresMandate: Boolean = false,
    ): TransformSpecToElements {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            StripeR.style.StripeDefaultTheme
        )

        return TransformSpecToElements(
            UiDefinitionFactory.Arguments(
                initialValues = mapOf(),
                initialLinkUserInput = null,
                saveForFutureUseInitialValue = true,
                merchantName = "Merchant, Inc.",
                cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
                shippingValues = null,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                requiresMandate = requiresMandate,
                linkConfigurationCoordinator = null,
                onLinkInlineSignupStateChanged = { throw AssertionError("Not implemented") },
                cardBrandFilter = DefaultCardBrandFilter
            )
        )
    }
}

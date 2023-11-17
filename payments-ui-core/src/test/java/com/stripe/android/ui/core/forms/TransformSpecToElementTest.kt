package com.stripe.android.ui.core.forms

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.Capitalization
import com.stripe.android.ui.core.elements.ContactInformationSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.EmptyFormElement
import com.stripe.android.ui.core.elements.KeyboardType
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.elements.TranslationId
import com.stripe.android.ui.core.elements.UpiElement
import com.stripe.android.ui.core.elements.UpiSpec
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
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import com.stripe.android.core.R as CoreR
import com.stripe.android.stripecardscan.R as CardScanR

@RunWith(RobolectricTestRunner::class)
internal class TransformSpecToElementTest {

    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        CardScanR.style.StripeCardScanDefaultTheme
    )

    private val nameSection = NameSpec()

    private val emailSection = EmailSpec()

    private lateinit var transformSpecToElements: TransformSpecToElements

    @Before
    fun beforeTest() {
        transformSpecToElements =
            TransformSpecToElements(
                addressRepository = mock(),
                initialValues = mapOf(),
                amount = null,
                saveForFutureUseInitialValue = true,
                merchantName = "Merchant, Inc.",
                context = context,
                shippingValues = null,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible
            )
    }

    @Test
    fun `Adding a country section sets up the section and country elements correctly`() =
        runBlocking {
            val countrySection = CountrySpec(allowedCountryCodes = setOf("AT"))
            val formElement = transformSpecToElements.transform(
                listOf(countrySection)
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
                listOf(idealSection)
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
            listOf(nameSection)
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
            )
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
            listOf(emailSection)
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
            listOf(staticText)
        )

        val staticTextElement = formElement.first() as StaticTextElement

        assertThat(staticTextElement.controller).isNull()
        assertThat(staticTextElement.stringResId).isEqualTo(staticText.stringResId)
        assertThat(staticTextElement.identifier).isEqualTo(staticText.apiPath)
    }

    @Test
    fun `Empty spec is transformed to single EmptyFormElement`() {
        val formElement = transformSpecToElements.transform(
            emptyList()
        )

        assertThat(formElement).containsExactly(EmptyFormElement())
    }

    @Test
    fun `UPI spec is transformed into UPI element wrapped in section`() {
        val upiSpec = UpiSpec()
        val formElement = transformSpecToElements.transform(listOf(upiSpec))

        val sectionElement = formElement.first() as SectionElement
        val upiElement = sectionElement.fields.first() as UpiElement

        assertThat(sectionElement.fields).containsExactly(upiElement)
    }

    @Test
    fun `Add a phone section spec sets up the phone element correctly`() = runBlocking {
        val phoneSpec = PhoneSpec()
        val formElement = transformSpecToElements.transform(
            listOf(phoneSpec)
        )

        val phoneSectionElement = formElement.first() as SectionElement
        val phoneElement = phoneSectionElement.fields[0] as PhoneNumberElement

        // Verify the correct config is setup for the controller
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `Add a contact information spec sets up the elements correctly`() = runBlocking {
        val contactInfoSpec = ContactInformationSpec(
            collectName = true,
            collectPhone = false,
            collectEmail = true,
        )
        val formElement = transformSpecToElements.transform(
            listOf(contactInfoSpec)
        )

        val sectionElement = formElement.first() as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(2)
        val nameElement = sectionElement.fields[0] as SimpleTextElement
        val emailElement = sectionElement.fields[1] as EmailElement

        // Verify the correct config is setup for the controller
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")
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

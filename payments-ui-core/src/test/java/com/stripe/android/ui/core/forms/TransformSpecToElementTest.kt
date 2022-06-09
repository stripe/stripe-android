package com.stripe.android.ui.core.forms

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.Capitalization
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.CardNumberViewOnlyController
import com.stripe.android.ui.core.elements.CountryConfig
import com.stripe.android.ui.core.elements.CountryElement
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailConfig
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KeyboardType
import com.stripe.android.ui.core.elements.NameConfig
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.ui.core.elements.SimpleTextElement
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.elements.TranslationId
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.StaticResourceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TransformSpecToElementTest {

    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )

    private val nameSection = NameSpec()

    private val emailSection = EmailSpec()

    private lateinit var transformSpecToElements: TransformSpecToElements

    @Before
    fun beforeTest() {
        transformSpecToElements =
            TransformSpecToElements(
                resourceRepository = StaticResourceRepository(
                    mock(),
                    mock()
                ),
                initialValues = mapOf(),
                amount = null,
                saveForFutureUseInitialValue = true,
                merchantName = "Merchant, Inc.",
                context
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
            assertThat(idealElement.controller.label.first()).isEqualTo(R.string.ideal_bank)

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
        assertThat(nameElement.controller.label.first()).isEqualTo(R.string.address_label_name)
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
            stringResId = R.string.sepa_mandate
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
    fun `Setting card number to view only returns the correct elements`() {
        transformSpecToElements =
            TransformSpecToElements(
                resourceRepository = StaticResourceRepository(
                    mock(),
                    mock()
                ),
                initialValues = mapOf(),
                amount = null,
                saveForFutureUseInitialValue = true,
                merchantName = "Merchant, Inc.",
                context,
                viewOnlyFields = setOf(IdentifierSpec.CardNumber)
            )

        val formElements = transformSpecToElements.transform(
            LpmRepository.HardcodedCard.formSpec.items
        )
        val cardDetailsSection = formElements.first() as CardDetailsSectionElement
        val cardNumberElement =
            cardDetailsSection.controller.cardDetailsElement.controller.numberElement

        assertThat(cardNumberElement.controller)
            .isInstanceOf(CardNumberViewOnlyController::class.java)
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

package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.BankDropdownSpec
import com.stripe.android.paymentsheet.elements.CountryElement
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.EmailElement
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.MandateTextElement
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.SaveForFutureUseElement
import com.stripe.android.paymentsheet.elements.SectionElement
import com.stripe.android.paymentsheet.elements.SimpleDropdownElement
import com.stripe.android.paymentsheet.elements.SimpleTextElement
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.elements.BankRepository
import com.stripe.android.paymentsheet.elements.CountrySpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.MandateTextSpec
import com.stripe.android.paymentsheet.elements.ResourceRepository
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.SupportedBankType
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File

internal class TransformSpecToElementTest {

    private val nameSection = SectionSpec(
        IdentifierSpec.Generic("name_section"),
        SimpleTextSpec.NAME
    )

    private val emailSection = SectionSpec(
        IdentifierSpec.Generic("email_section"),
        EmailSpec
    )

    private lateinit var transformSpecToElement: TransformSpecToElement

    @Before
    fun beforeTest() {
        val bankRepository = BankRepository(mock())
        bankRepository.init(
            mapOf(SupportedBankType.Ideal to IDEAL_BANKS_JSON.byteInputStream())
        )

        transformSpecToElement =
            TransformSpecToElement(
                ResourceRepository(
                    bankRepository,
                    AddressFieldElementRepository(mock())
                ),
                FormFragmentArguments(
                    supportedPaymentMethod = SupportedPaymentMethod.Card,
                    displayUIRequiredForSaving = true,
                    intentAndPmAllowUserInitiatedReuse = true,
                    merchantName = "Example, Inc."
                )
            )
    }

    @Test
    fun `Section with multiple fields contains all fields in the section element`() {
        val formElement = transformSpecToElement.transform(
            listOf(
                SectionSpec(
                    IdentifierSpec.Generic("multifield_section"),
                    listOf(
                        CountrySpec(),
                        IDEAL_BANK_CONFIG
                    )
                )
            )
        )

        val sectionElement = formElement[0] as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(2)
        assertThat(sectionElement.fields[0]).isInstanceOf(CountryElement::class.java)
        assertThat(sectionElement.fields[1]).isInstanceOf(SimpleDropdownElement::class.java)
    }

    @Test
    fun `Adding a country section sets up the section and country elements correctly`() {
        val countrySection = SectionSpec(
            IdentifierSpec.Generic("country_section"),
            CountrySpec(onlyShowCountryCodes = setOf("AT"))
        )
        val formElement = transformSpecToElement.transform(
            listOf(countrySection)
        )

        val countrySectionElement = formElement.first() as SectionElement
        val countryElement = countrySectionElement.fields[0] as CountryElement

        assertThat(countryElement.controller.displayItems).hasSize(1)
        assertThat(countryElement.controller.displayItems[0]).isEqualTo("Austria")

        // Verify the correct config is setup for the controller
        assertThat(countryElement.controller.label).isEqualTo(CountryConfig().label)

        assertThat(countrySectionElement.identifier.value).isEqualTo("country_section")

        assertThat(countryElement.identifier.value).isEqualTo("country")
    }

    @Test
    fun `Adding a ideal bank section sets up the section and country elements correctly`() {
        val idealSection = SectionSpec(
            IdentifierSpec.Generic("ideal_section"),
            IDEAL_BANK_CONFIG
        )
        val formElement = transformSpecToElement.transform(
            listOf(idealSection)
        )

        val idealSectionElement = formElement.first() as SectionElement
        val idealElement = idealSectionElement.fields[0] as SimpleDropdownElement

        // Verify the correct config is setup for the controller
        assertThat(idealElement.controller.label).isEqualTo(R.string.stripe_paymentsheet_ideal_bank)

        assertThat(idealSectionElement.identifier.value).isEqualTo("ideal_section")

        assertThat(idealElement.identifier.value).isEqualTo("bank")
    }

    @Test
    fun `Add a name section spec sets up the name element correctly`() {
        val formElement = transformSpecToElement.transform(
            listOf(nameSection)
        )

        val nameElement = (formElement.first() as SectionElement)
            .fields[0] as SimpleTextElement

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label).isEqualTo(NameConfig().label)
        assertThat(nameElement.identifier.value).isEqualTo("name")

        assertThat(nameElement.controller.capitalization).isEqualTo(KeyboardCapitalization.Words)
        assertThat(nameElement.controller.keyboardType).isEqualTo(KeyboardType.Text)
    }

    @Test
    fun `Add a simple text section spec sets up the text element correctly`() {
        val formElement = transformSpecToElement.transform(
            listOf(
                SectionSpec(
                    IdentifierSpec.Generic("simple_section"),
                    SimpleTextSpec(
                        IdentifierSpec.Generic("simple"),
                        R.string.address_label_name,
                        showOptionalLabel = true,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    )
                )
            )
        )

        val nameElement = (formElement.first() as SectionElement).fields[0]
            as SimpleTextElement

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label).isEqualTo(R.string.address_label_name)
        assertThat(nameElement.identifier.value).isEqualTo("simple")
        assertThat(nameElement.controller.showOptionalLabel).isTrue()
    }

    @Test
    fun `Add a email section spec sets up the email element correctly`() {
        val formElement = transformSpecToElement.transform(
            listOf(emailSection)
        )

        val emailSectionElement = formElement.first() as SectionElement
        val emailElement = emailSectionElement.fields[0] as EmailElement

        // Verify the correct config is setup for the controller
        assertThat(emailElement.controller.label).isEqualTo(EmailConfig().label)
        assertThat(emailElement.identifier.value).isEqualTo("email")
    }

    @Test
    fun `Add a mandate section spec setup of the mandate element correctly`() {
        val mandate = MandateTextSpec(
            IdentifierSpec.Generic("mandate"),
            R.string.stripe_paymentsheet_sepa_mandate,
            Color.Gray
        )
        val formElement = transformSpecToElement.transform(
            listOf(mandate)
        )

        val mandateElement = formElement.first() as MandateTextElement

        assertThat(mandateElement.controller).isNull()
        assertThat(mandateElement.color).isEqualTo(mandate.color)
        assertThat(mandateElement.stringResId).isEqualTo(mandate.stringResId)
        assertThat(mandateElement.identifier).isEqualTo(mandate.identifier)
    }

    @Test
    fun `Add a save for future use section spec sets the mandate element correctly`() =
        runBlocking {
            val mandate = MandateTextSpec(
                IdentifierSpec.Generic("mandate"),
                R.string.stripe_paymentsheet_sepa_mandate,
                Color.Gray
            )
            val hiddenIdentifiers = listOf(nameSection, mandate)
            val saveForFutureUseSpec = SaveForFutureUseSpec(hiddenIdentifiers)
            val formElement = transformSpecToElement.transform(
                listOf(saveForFutureUseSpec)
            )

            val saveForFutureUseElement =
                formElement.first() as SaveForFutureUseElement
            val saveForFutureUseController = saveForFutureUseElement.controller

            assertThat(saveForFutureUseElement.identifier)
                .isEqualTo(saveForFutureUseSpec.identifier)

            assertThat(saveForFutureUseController.hiddenIdentifiers.first()).isEmpty()

            saveForFutureUseController.onValueChange(false)
            assertThat(saveForFutureUseController.hiddenIdentifiers.first())
                .isEqualTo(
                    hiddenIdentifiers.map { it.identifier }
                )
        }

    companion object {
        val IDEAL_BANK_CONFIG = BankDropdownSpec(
            IdentifierSpec.Generic("bank"),
            R.string.stripe_paymentsheet_ideal_bank,
            SupportedBankType.Ideal
        )

        val IDEAL_BANKS_JSON =
            File("src/main/assets/idealBanks.json")
                .inputStream()
                .bufferedReader()
                .use { it.readText() }
    }
}

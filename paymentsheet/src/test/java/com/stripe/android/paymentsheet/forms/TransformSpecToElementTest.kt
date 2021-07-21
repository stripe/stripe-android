package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FocusRequesterCount
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.FormElement.MandateTextElement
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.SectionFieldElement.Country
import com.stripe.android.paymentsheet.SectionFieldElement.Email
import com.stripe.android.paymentsheet.SectionFieldElement.IdealBank
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.IdealBankConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TransformSpecToElementTest {

    private val nameSection = FormItemSpec.SectionSpec(
        IdentifierSpec("nameSection"),
        SectionFieldSpec.NAME
    )

    private val emailSection = FormItemSpec.SectionSpec(
        IdentifierSpec("emailSection"),
        SectionFieldSpec.Email
    )

    @Test
    fun `Section with multiple fields contains all fields in the section element`() {
        val formElement = listOf(
            FormItemSpec.SectionSpec(
                IdentifierSpec("multifieldSection"),
                listOf(
                    SectionFieldSpec.Country(),
                    SectionFieldSpec.IdealBank
                )
            )
        ).transform(
            "Example, Inc.",
            FocusRequesterCount()
        )

        val sectionElement = formElement[0] as SectionElement
        assertThat(sectionElement.fields.size).isEqualTo(2)
        assertThat(sectionElement.fields[0]).isInstanceOf(Country::class.java)
        assertThat(sectionElement.fields[1]).isInstanceOf(IdealBank::class.java)
    }

    @Test
    fun `Adding a country section sets up the section and country elements correctly`() {
        val countrySection = FormItemSpec.SectionSpec(
            IdentifierSpec("countrySection"),
            SectionFieldSpec.Country(onlyShowCountryCodes = setOf("AT"))
        )
        val formElement = listOf(countrySection).transform(
            "Example, Inc.",
            FocusRequesterCount()
        )

        val countrySectionElement = formElement.first() as SectionElement
        val countryElement = countrySectionElement.fields[0] as Country

        assertThat(countryElement.controller.displayItems).hasSize(1)
        assertThat(countryElement.controller.displayItems[0]).isEqualTo("Austria")

        // Verify the correct config is setup for the controller
        assertThat(countryElement.controller.label).isEqualTo(CountryConfig().label)

        assertThat(countrySectionElement.identifier.value).isEqualTo("countrySection")

        assertThat(countryElement.identifier.value).isEqualTo("country")
    }

    @Test
    fun `Adding a ideal bank section sets up the section and country elements correctly`() {
        val idealSection = FormItemSpec.SectionSpec(
            IdentifierSpec("idealSection"),
            SectionFieldSpec.IdealBank
        )
        val formElement = listOf(idealSection).transform(
            "Example, Inc.",
            FocusRequesterCount()
        )

        val idealSectionElement = formElement.first() as SectionElement
        val idealElement = idealSectionElement.fields[0] as IdealBank

        // Verify the correct config is setup for the controller
        assertThat(idealElement.controller.label).isEqualTo(IdealBankConfig().label)

        assertThat(idealSectionElement.identifier.value).isEqualTo("idealSection")

        assertThat(idealElement.identifier.value).isEqualTo("bank")
    }

    @Test
    fun `Add a name section spec sets up the name element correctly`() {
        val formElement = listOf(nameSection).transform(
            "Example, Inc.",
            FocusRequesterCount()
        )

        val nameElement =
            (formElement.first() as SectionElement).fields[0] as SectionFieldElement.SimpleText

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label).isEqualTo(NameConfig().label)
        assertThat(nameElement.identifier.value).isEqualTo("name")

        assertThat(nameElement.controller.capitalization).isEqualTo(KeyboardCapitalization.Words)
        assertThat(nameElement.controller.keyboardType).isEqualTo(KeyboardType.Text)
    }

    @Test
    fun `Add a simple text section spec sets up the text element correctly`() {
        val formElement = listOf(
            FormItemSpec.SectionSpec(
                IdentifierSpec("simple_section"),
                SectionFieldSpec.SimpleText(
                    IdentifierSpec("simple"),
                    R.string.address_label_name,
                    showOptionalLabel = true,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                )
            )
        ).transform(
            "Example, Inc.",
            FocusRequesterCount()
        )

        val nameElement = (formElement.first() as SectionElement).fields[0]
            as SectionFieldElement.SimpleText

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label).isEqualTo(R.string.address_label_name)
        assertThat(nameElement.identifier.value).isEqualTo("simple")
        assertThat(nameElement.controller.showOptionalLabel).isTrue()
    }

    @Test
    fun `Add a email section spec sets up the email element correctly`() {
        val formElement = listOf(emailSection).transform(
            "Example, Inc.",
            FocusRequesterCount()
        )

        val emailSectionElement = formElement.first() as SectionElement
        val emailElement = emailSectionElement.fields[0] as Email

        // Verify the correct config is setup for the controller
        assertThat(emailElement.controller.label).isEqualTo(EmailConfig().label)
        assertThat(emailElement.identifier.value).isEqualTo("email")
    }

    @Test
    fun `Adding to sections that get focus sets up the focus indexes correctly`() {
        val focusRequesterCount = FocusRequesterCount()
        val formElement = listOf(nameSection, emailSection).transform(
            "Example, Inc.",
            focusRequesterCount
        )

        val nameSectionElement = formElement[0] as SectionElement
        val nameElement = nameSectionElement.fields[0] as SectionFieldElement.SimpleText
        val emailSectionElement = formElement[1] as SectionElement
        val emailElement = emailSectionElement.fields[0] as Email

        assertThat(nameElement.focusIndexOrder).isEqualTo(0)
        assertThat(emailElement.focusIndexOrder).isEqualTo(1)

        // It should equal as many text fields as are present
        assertThat(focusRequesterCount.get()).isEqualTo(2)
    }

    @Test
    fun `Add a mandate section spec setup of the mandate element correctly`() {
        val mandate = FormItemSpec.MandateTextSpec(
            IdentifierSpec("mandate"),
            R.string.stripe_paymentsheet_sepa_mandate,
            Color.Gray
        )
        val formElement = listOf(mandate).transform(
            "Example, Inc.",
            FocusRequesterCount()
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
            val mandate = FormItemSpec.MandateTextSpec(
                IdentifierSpec("mandate"),
                R.string.stripe_paymentsheet_sepa_mandate,
                Color.Gray
            )
            val optionalIdentifiers = listOf(nameSection, mandate)
            val saveForFutureUseSpec = FormItemSpec.SaveForFutureUseSpec(optionalIdentifiers)
            val formElement = listOf(saveForFutureUseSpec).transform(
                "Example, Inc.",
                FocusRequesterCount()
            )

            val saveForFutureUseElement = formElement.first() as FormElement.SaveForFutureUseElement
            val saveForFutureUseController = saveForFutureUseElement.controller

            assertThat(saveForFutureUseElement.identifier)
                .isEqualTo(saveForFutureUseSpec.identifier)

            assertThat(saveForFutureUseController.optionalIdentifiers.first()).isEmpty()

            saveForFutureUseController.onValueChange(false)
            assertThat(saveForFutureUseController.optionalIdentifiers.first())
                .isEqualTo(
                    optionalIdentifiers.map { it.identifier }
                )
        }
}

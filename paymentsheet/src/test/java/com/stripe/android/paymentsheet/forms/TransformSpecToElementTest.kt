package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FocusRequesterCount
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.FormElement.MandateTextElement
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.SectionFieldElement.Country
import com.stripe.android.paymentsheet.SectionFieldElement.Email
import com.stripe.android.paymentsheet.SectionFieldElement.Name
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.IdealBankConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TransformSpecToElementTest {

    private val transformSpecToElement = TransformSpecToElement()

    private val nameSection = FormItemSpec.SectionSpec(
        IdentifierSpec("nameSection"),
        SectionFieldSpec.Name
    )

    private val emailSection = FormItemSpec.SectionSpec(
        IdentifierSpec("emailSection"),
        SectionFieldSpec.Email
    )

    @Test
    fun `Adding a country section sets up the section and country elements correctly`() {
        val countrySection = FormItemSpec.SectionSpec(
            IdentifierSpec("countrySection"),
            SectionFieldSpec.Country
        )
        val formElement = transformSpecToElement.transform(
            LayoutSpec(
                listOf(countrySection)
            ),
            "Example, Inc.",
            FocusRequesterCount()
        )

        val countrySectionElement = formElement.first() as SectionElement
        val countryElement = countrySectionElement.field as Country

        // With only a single field in a section the section controller is just a pass through
        // of the section field controller
        assertThat(countrySectionElement.controller).isEqualTo(countryElement.controller)

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
        val formElement = transformSpecToElement.transform(
            LayoutSpec(
                listOf(idealSection)
            ),
            "Example, Inc.",
            FocusRequesterCount()
        )

        val idealSectionElement = formElement.first() as SectionElement
        val idealElement = idealSectionElement.field as SectionFieldElement.IdealBank

        // With only a single field in a section the section controller is just a pass through
        // of the section field controller
        assertThat(idealSectionElement.controller).isEqualTo(idealElement.controller)

        // Verify the correct config is setup for the controller
        assertThat(idealElement.controller.label).isEqualTo(IdealBankConfig().label)

        assertThat(idealSectionElement.identifier.value).isEqualTo("idealSection")

        assertThat(idealElement.identifier.value).isEqualTo("bank")
    }

    @Test
    fun `Add a name section spec sets up the name element correctly`() {
        val formElement = transformSpecToElement.transform(
            LayoutSpec(
                listOf(nameSection)
            ),
            "Example, Inc.",
            FocusRequesterCount()
        )

        val nameElement = (formElement.first() as SectionElement).field as Name

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label).isEqualTo(NameConfig().label)
        assertThat(nameElement.identifier.value).isEqualTo("name")
    }

    @Test
    fun `Add a email section spec sets up the email element correctly`() {
        val formElement = transformSpecToElement.transform(
            LayoutSpec(
                listOf(emailSection)
            ),
            "Example, Inc.",
            FocusRequesterCount()
        )

        val emailSectionElement = formElement.first() as SectionElement
        val emailElement = emailSectionElement.field as Email

        // Verify the correct config is setup for the controller
        assertThat(emailElement.controller.label).isEqualTo(EmailConfig().label)
        assertThat(emailElement.identifier.value).isEqualTo("email")
    }

    @Test
    fun `Adding to sections that get focus sets up the focus indexes correctly`() {
        val focusRequesterCount = FocusRequesterCount()
        val formElement = transformSpecToElement.transform(
            LayoutSpec(
                listOf(nameSection, emailSection)
            ),
            "Example, Inc.",
            focusRequesterCount
        )

        val nameSectionElement = formElement[0] as SectionElement
        val nameElement = nameSectionElement.field as Name
        val emailSectionElement = formElement[1] as SectionElement
        val emailElement = emailSectionElement.field as Email

        assertThat(nameElement.focusIndexOrder).isEqualTo(0)
        assertThat(emailElement.focusIndexOrder).isEqualTo(1)

        // It should equal as many text fields as are present
        assertThat(focusRequesterCount.get()).isEqualTo(2)
    }

    @Test
    fun `Add a mandate section spec setup of the mandate element correctly`() {
        val mandate = FormItemSpec.MandateTextSpec(
            IdentifierSpec("mandate"),
            R.string.sofort_mandate,
            Color.Gray
        )
        val formElement = transformSpecToElement.transform(
            LayoutSpec(
                listOf(mandate)
            ),
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
                R.string.sofort_mandate,
                Color.Gray
            )
            val optionalIdentifiers = listOf(nameSection, mandate)
            val saveForFutureUseSpec = FormItemSpec.SaveForFutureUseSpec(optionalIdentifiers)
            val formElement = transformSpecToElement.transform(
                LayoutSpec(
                    listOf(saveForFutureUseSpec)
                ),
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

package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FocusRequesterCount
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.FormElement.SectionElement.SectionFieldElement.Name
import com.stripe.android.paymentsheet.FormElement.SectionElement.SectionFieldElement.Email
import com.stripe.android.paymentsheet.FormElement.SectionElement.SectionFieldElement.Country
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import org.junit.Test

class TransformSpecToElementTest {

    private val transformSpecToElement = TransformSpecToElement()
    private val focusRequesterCount = FocusRequesterCount()

    @Test
    fun `Create a layout element`() {
        val layout = LayoutSpec(
            listOf(
                FormItemSpec.SectionSpec(
                    IdentifierSpec("nameSection"),
                    FormItemSpec.SectionSpec.SectionFieldSpec.Name
                ),
                FormItemSpec.SectionSpec(
                    IdentifierSpec("emailSection"),
                    FormItemSpec.SectionSpec.SectionFieldSpec.Email
                ),
                FormItemSpec.SectionSpec(
                    IdentifierSpec("countrySection"),
                    FormItemSpec.SectionSpec.SectionFieldSpec.Country
                ),
                FormItemSpec.StaticTextSpec(
                    IdentifierSpec("mandateSection"),
                    R.string.sofort_mandate,
                    Color.Gray
                )
            )
        )

        val formElement = transformSpecToElement.transform(layout, focusRequesterCount)
        val nameSectionElement = (formElement[0] as SectionElement)
        val nameElement = nameSectionElement.field as Name
        val emailSectionElement = (formElement[1] as SectionElement)
        val emailElement = emailSectionElement.field as Email
        val countrySectionElement = (formElement[2] as SectionElement)
        val countryElement = countrySectionElement.field as Country

        // With only a single field in a section the section controller is just a pass through
        // of the section field controller
        assertThat(nameSectionElement.controller).isEqualTo(nameElement.controller)
        assertThat(emailSectionElement.controller).isEqualTo(emailElement.controller)
        assertThat(countrySectionElement.controller).isEqualTo(countryElement.controller)

        // Verify the correct config is setup for the controller
        assertThat(nameElement.controller.label).isEqualTo(NameConfig().label)
        assertThat(emailElement.controller.label).isEqualTo(EmailConfig().label)
        assertThat(countryElement.controller.label).isEqualTo(CountryConfig().label)

        assertThat(nameSectionElement.identifier.value).isEqualTo("nameSection")
        assertThat(emailSectionElement.identifier.value).isEqualTo("emailSection")
        assertThat(countrySectionElement.identifier.value).isEqualTo("countrySection")

        assertThat(nameElement.identifier.value).isEqualTo("name")
        assertThat(emailElement.identifier.value).isEqualTo("email")
        assertThat(countryElement.identifier.value).isEqualTo("country")

        assertThat(nameElement.focusIndexOrder).isEqualTo(0)
        assertThat(emailElement.focusIndexOrder).isEqualTo(1)

        // It should equal as many text field as are present
        assertThat(focusRequesterCount.get()).isEqualTo(2)

    }
}
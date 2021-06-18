package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TransformElementToFormViewValueFlowTest {

    private val emailController = TextFieldController(EmailConfig())
    private val emailSection = FormElement.SectionElement(
        identifier = IdentifierSpec("emailSection"),
        FormElement.SectionElement.SectionFieldElement.Email(
            IdentifierSpec("email"),
            emailController,
            0
        ),
        emailController
    )

    private val countryController = DropdownFieldController(CountryConfig())
    private val countrySection = FormElement.SectionElement(
        identifier = IdentifierSpec("countrySection"),
        FormElement.SectionElement.SectionFieldElement.Country(
            IdentifierSpec("country"),
            countryController
        ),
        countryController
    )

    private val optionalIdentifersFlow = MutableStateFlow<List<IdentifierSpec>>(emptyList())

    private val transformElementToFormFieldValueFlow = TransformElementToFormFieldValueFlow(
        listOf(countrySection, emailSection),
        optionalIdentifersFlow
    )

    @Test
    fun `Verify with only some complete controllers and no optional values the flow value is null`() {
        runBlocking {
            assertThat(transformElementToFormFieldValueFlow.transformFlow().first()).isNull()
        }
    }

    @Test
    fun `Verify if all controllers are complete and no optional values the flow value it has the values`() {
        runBlocking {
            emailController.onValueChange("email@valid.com")

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs).containsKey("email")
            assertThat(formFieldValue?.fieldValuePairs).containsKey("country")
        }
    }

    @Test
    fun `Verify if an optional field is incomplete the field pairs still have the non-optional values`() {
        runBlocking {
            emailController.onValueChange("email is invalid")
            optionalIdentifersFlow.value = listOf(emailSection.identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs).doesNotContainKey("email")
            assertThat(formFieldValue?.fieldValuePairs).containsKey("country")
        }
    }

    @Test
    fun `Verify if an optional field is complete the field pairs still have only the non-optional values`() {
        runBlocking {
            emailController.onValueChange("email@valid.com")
            optionalIdentifersFlow.value = listOf(emailSection.identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs).doesNotContainKey("email")
            assertThat(formFieldValue?.fieldValuePairs).containsKey("country")
        }
    }

}
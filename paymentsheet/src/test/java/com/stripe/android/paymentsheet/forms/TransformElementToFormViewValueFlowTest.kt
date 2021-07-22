package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TransformElementToFormViewValueFlowTest {

    private val emailController = TextFieldController(EmailConfig())
    private val emailSection = FormElement.SectionElement(
        identifier = IdentifierSpec("emailSection"),
        SectionFieldElement.Email(
            IdentifierSpec("email"),
            emailController
        ),
        SectionController(emailController.label, listOf(emailController))
    )

    private val countryController = DropdownFieldController(CountryConfig())
    private val countrySection = FormElement.SectionElement(
        identifier = IdentifierSpec("countrySection"),
        SectionFieldElement.Country(
            IdentifierSpec("country"),
            countryController
        ),
        SectionController(countryController.label, listOf(countryController))
    )

    private val optionalIdentifersFlow = MutableStateFlow<List<IdentifierSpec>>(emptyList())

    private val transformElementToFormFieldValueFlow = TransformElementToFormFieldValueFlow(
        listOf(countrySection, emailSection),
        optionalIdentifersFlow,
        showingMandate = MutableStateFlow(true),
        saveForFutureUse = MutableStateFlow(false)
    )

    @Test
    fun `With only some complete controllers and no optional values the flow value is null`() {
        runBlocking {
            assertThat(transformElementToFormFieldValueFlow.transformFlow().first()).isNull()
        }
    }

    @Test
    fun `If all controllers are complete and no optional values the flow value has all values`() {
        runBlocking {
            emailController.onValueChange("email@valid.com")

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec("email"))
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec("country"))
        }
    }

    @Test
    fun `If an optional field is incomplete field pairs have the non-optional values`() {
        runBlocking {
            emailController.onValueChange("email is invalid")
            optionalIdentifersFlow.value = listOf(IdentifierSpec("email"))

            val formFieldValues = transformElementToFormFieldValueFlow.transformFlow()

            val formFieldValue = formFieldValues.first()
            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(IdentifierSpec("email"))
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec("country"))
        }
    }

    @Test
    fun `If an optional field is complete field pairs contain only the non-optional values`() {
        runBlocking {
            emailController.onValueChange("email@valid.com")
            optionalIdentifersFlow.value = listOf(emailSection.fields[0].identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(IdentifierSpec("email"))
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec("country"))
        }
    }
}

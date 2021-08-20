package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.Identifier
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.getIdInputControllerMap
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TransformElementToFormViewValueFlowTest {

    private val emailController = TextFieldController(EmailConfig())
    private val emailSection = FormElement.SectionElement(
        identifier = Identifier.Generic("emailSection"),
        SectionFieldElement.Email(
            Identifier.Email,
            emailController
        ),
        SectionController(emailController.label, listOf(emailController))
    )

    private val countryController = DropdownFieldController(CountryConfig())
    private val countrySection = FormElement.SectionElement(
        identifier = Identifier.Generic("countrySection"),
        SectionFieldElement.Country(
            Identifier.Country,
            countryController
        ),
        SectionController(countryController.label, listOf(countryController))
    )

    private val hiddenIdentifersFlow = MutableStateFlow<List<Identifier>>(emptyList())

    private val transformElementToFormFieldValueFlow = TransformElementToFormFieldValueFlow(
        listOf(countrySection, emailSection).getIdInputControllerMap(),
        hiddenIdentifersFlow,
        showingMandate = MutableStateFlow(true)
    )

    @Test
    fun `With only some complete controllers and no hidden values the flow value is null`() {
        runBlocking {
            assertThat(transformElementToFormFieldValueFlow.transformFlow().first()).isNull()
        }
    }

    @Test
    fun `If all controllers are complete and no hidden values the flow value has all values`() {
        runBlocking {
            emailController.onValueChange("email@valid.com")

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(Identifier.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(Identifier.Country)
        }
    }

    @Test
    fun `If an hidden field is incomplete field pairs have the non-hidden values`() {
        runBlocking {
            emailController.onValueChange("email is invalid")
            hiddenIdentifersFlow.value = listOf(Identifier.Email)

            val formFieldValues = transformElementToFormFieldValueFlow.transformFlow()

            val formFieldValue = formFieldValues.first()
            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(Identifier.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(Identifier.Country)
        }
    }

    @Test
    fun `If an hidden field is complete field pairs contain only the non-hidden values`() {
        runBlocking {
            emailController.onValueChange("email@valid.com")
            hiddenIdentifersFlow.value = listOf(emailSection.fields[0].identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(Identifier.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(Identifier.Country)
        }
    }
}

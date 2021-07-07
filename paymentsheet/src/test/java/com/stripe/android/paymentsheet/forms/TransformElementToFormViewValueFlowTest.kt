package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.SectionFieldElement
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
        SectionFieldElement.Email(
            IdentifierSpec("email"),
            emailController,
            0
        ),
        emailController
    )

    private val countryController = DropdownFieldController(CountryConfig())
    private val countrySection = FormElement.SectionElement(
        identifier = IdentifierSpec("countrySection"),
        SectionFieldElement.Country(
            IdentifierSpec("country"),
            countryController
        ),
        countryController
    )

    private val optionalIdentifersFlow = MutableStateFlow<Set<IdentifierSpec>>(emptySet())

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
                .containsKey(IdentifierSpec("emailSection"))
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec("countrySection"))
        }
    }

    @Test
    fun `If an optional field is incomplete field pairs have the non-optional values`() {
        runBlocking {
            emailController.onValueChange("email is invalid")
            optionalIdentifersFlow.value = setOf(emailSection.identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(IdentifierSpec("emailSection"))
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec("countrySection"))
        }
    }

    @Test
    fun `If an optional field is complete field pairs contain only the non-optional values`() {
        runBlocking {
            emailController.onValueChange("email@valid.com")
            optionalIdentifersFlow.value = setOf(emailSection.identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.transformFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(IdentifierSpec("emailSection"))
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec("countrySection"))
        }
    }
}

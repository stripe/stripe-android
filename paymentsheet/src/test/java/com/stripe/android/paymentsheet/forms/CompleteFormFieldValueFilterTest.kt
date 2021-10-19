package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.EmailElement
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SectionElement
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class CompleteFormFieldValueFilterTest {

    private val emailController = TextFieldController(EmailConfig())
    private val emailSection = SectionElement(
        identifier = IdentifierSpec.Generic("email_section"),
        EmailElement(
            IdentifierSpec.Email,
            emailController
        ),
        SectionController(emailController.label, listOf(emailController))
    )

    private val hiddenIdentifersFlow = MutableStateFlow<List<IdentifierSpec>>(emptyList())

    private val fieldFlow = MutableStateFlow(
        mapOf(
            IdentifierSpec.Country to FormFieldEntry("US", true),
            IdentifierSpec.Email to FormFieldEntry("email@email.com", false),
        )
    )

    private val transformElementToFormFieldValueFlow = CompleteFormFieldValueFilter(
        fieldFlow,
        hiddenIdentifersFlow,
        showingMandate = MutableStateFlow(true),
        userRequestedReuse = MutableStateFlow(PaymentSelection.CustomerRequestedSave.NoRequest)
    )

    @Test
    fun `With only some complete controllers and no hidden values the flow value is null`() {
        runBlockingTest {
            assertThat(transformElementToFormFieldValueFlow.filterFlow().first()).isNull()
        }
    }

    @Test
    fun `If all controllers are complete and no hidden values the flow value has all values`() {
        runBlockingTest {
            fieldFlow.value =
                mapOf(
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                    IdentifierSpec.Email to FormFieldEntry("email@email.com", true),
                )

            val formFieldValue = transformElementToFormFieldValueFlow.filterFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec.Country)
        }
    }

    @Test
    fun `If an hidden field is incomplete field pairs have the non-hidden values`() {
        runBlockingTest {
            hiddenIdentifersFlow.value = listOf(IdentifierSpec.Email)

            val formFieldValues = transformElementToFormFieldValueFlow.filterFlow()

            val formFieldValue = formFieldValues.first()
            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(IdentifierSpec.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec.Country)
        }
    }

    @Test
    fun `If an hidden field is complete field pairs contain only the non-hidden values`() {
        runBlockingTest {
            fieldFlow.value =
                mapOf(
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                    IdentifierSpec.Email to FormFieldEntry("email@email.com", true),
                )

            hiddenIdentifersFlow.value = listOf(emailSection.fields[0].identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.filterFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(IdentifierSpec.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec.Country)
        }
    }
}

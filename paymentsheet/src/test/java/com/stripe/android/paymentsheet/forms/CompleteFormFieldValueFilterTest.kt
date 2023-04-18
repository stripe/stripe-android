package com.stripe.android.paymentsheet.forms

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompleteFormFieldValueFilterTest {

    private val emailController = SimpleTextFieldController(EmailConfig())
    private var emailSection: SectionElement

    private val hiddenIdentifersFlow = MutableStateFlow<Set<IdentifierSpec>>(emptySet())

    private val fieldFlow = MutableStateFlow(
        mapOf(
            IdentifierSpec.Country to FormFieldEntry("US", true),
            IdentifierSpec.Email to FormFieldEntry("email@email.com", false)
        )
    )

    private val transformElementToFormFieldValueFlow = CompleteFormFieldValueFilter(
        fieldFlow,
        hiddenIdentifersFlow,
        showingMandate = MutableStateFlow(true),
        userRequestedReuse = MutableStateFlow(PaymentSelection.CustomerRequestedSave.NoRequest),
        defaultValues = emptyMap(),
    )

    init {
        runBlocking {
            emailSection = SectionElement(
                identifier = IdentifierSpec.Generic("email_section"),
                EmailElement(
                    IdentifierSpec.Email,
                    controller = emailController
                ),
                SectionController(emailController.label.first(), listOf(emailController))
            )
        }
    }

    @Test
    fun `With only some complete controllers and no hidden values the flow value is null`() {
        runTest {
            assertThat(transformElementToFormFieldValueFlow.filterFlow().first()).isNull()
        }
    }

    @Test
    fun `If all controllers are complete and no hidden values the flow value has all values`() {
        runTest {
            fieldFlow.value =
                mapOf(
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                    IdentifierSpec.Email to FormFieldEntry("email@email.com", true)
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
        runTest {
            hiddenIdentifersFlow.value = setOf(IdentifierSpec.Email)

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
        runTest {
            fieldFlow.value =
                mapOf(
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                    IdentifierSpec.Email to FormFieldEntry("email@email.com", true)
                )

            hiddenIdentifersFlow.value = setOf(emailSection.fields[0].identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.filterFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(IdentifierSpec.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(IdentifierSpec.Country)
        }
    }

    @Test
    fun `Verify defaults are preserved if no fields override them`() = runTest {
        val formFieldValueFilter = CompleteFormFieldValueFilter(
            fieldFlow,
            hiddenIdentifersFlow,
            showingMandate = MutableStateFlow(true),
            userRequestedReuse = MutableStateFlow(PaymentSelection.CustomerRequestedSave.NoRequest),
            defaultValues = mapOf(
                IdentifierSpec.Name to "Jane Doe",
                IdentifierSpec.Email to "foo@bar.com",
            ),
        )

        fieldFlow.value = mapOf(
            IdentifierSpec.Country to FormFieldEntry("US", true),
        )

        formFieldValueFilter.filterFlow().test {
            assertThat(awaitItem()?.fieldValuePairs).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jane Doe", true),
                    IdentifierSpec.Email to FormFieldEntry("foo@bar.com", true),
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                )
            )
        }
    }

    @Test
    fun `Verify defaults are overridden`() = runTest {
        val formFieldValueFilter = CompleteFormFieldValueFilter(
            fieldFlow,
            hiddenIdentifersFlow,
            showingMandate = MutableStateFlow(true),
            userRequestedReuse = MutableStateFlow(PaymentSelection.CustomerRequestedSave.NoRequest),
            defaultValues = mapOf(
                IdentifierSpec.Name to "Jane Doe",
                IdentifierSpec.Email to "foo@bar.com",
            ),
        )

        fieldFlow.value = mapOf(
            IdentifierSpec.Country to FormFieldEntry("US", true),
            IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", true),
            IdentifierSpec.Email to FormFieldEntry("mail@mail.com", true),
        )

        formFieldValueFilter.filterFlow().test {
            assertThat(awaitItem()?.fieldValuePairs).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", true),
                    IdentifierSpec.Email to FormFieldEntry("mail@mail.com", true),
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                )
            )
        }
    }
}

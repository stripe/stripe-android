package com.stripe.android.paymentsheet.forms

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FormFieldId
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

    private val hiddenIdentifersFlow = MutableStateFlow<Set<FormFieldId>>(emptySet())

    private val fieldFlow = MutableStateFlow(
        mapOf(
            FormFieldId.Country to FormFieldEntry("US", true),
            FormFieldId.Email to FormFieldEntry("email@email.com", false)
        )
    )

    private val transformElementToFormFieldValueFlow = CompleteFormFieldValueFilter(
        fieldFlow,
        hiddenIdentifersFlow,
        defaultValues = emptyMap(),
    )

    init {
        runBlocking {
            emailSection = SectionElement(
                identifier = FormFieldId.Generic("email_section"),
                EmailElement(
                    FormFieldId.Email,
                    controller = emailController
                ),
                SectionController(
                    emailController.label.first(),
                    listOf(emailController)
                )
            )
        }
    }

    @Test
    fun `save preference and field values come from the same snapshot`() = runTest {
        fieldFlow.value = mapOf(FormFieldId.SaveForFutureUse to FormFieldEntry("true", true))

        transformElementToFormFieldValueFlow.filterFlow().test {
            val optedIn = requireNotNull(awaitItem())
            assertThat(optedIn.userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.RequestReuse)
            assertThat(optedIn.fieldValuePairs[FormFieldId.SaveForFutureUse]?.value).isEqualTo("true")

            fieldFlow.value = mapOf(FormFieldId.SaveForFutureUse to FormFieldEntry("false", true))

            val optedOut = requireNotNull(awaitItem())
            assertThat(optedOut.userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.RequestNoReuse)
            assertThat(optedOut.fieldValuePairs[FormFieldId.SaveForFutureUse]?.value).isEqualTo("false")
            expectNoEvents()
        }
    }

    @Test
    fun `hidden save field still determines save preference`() = runTest {
        fieldFlow.value = mapOf(FormFieldId.SaveForFutureUse to FormFieldEntry("true", true))
        hiddenIdentifersFlow.value = setOf(FormFieldId.SaveForFutureUse)

        transformElementToFormFieldValueFlow.filterFlow().test {
            val values = requireNotNull(awaitItem())
            assertThat(values.userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.RequestReuse)
            assertThat(values.fieldValuePairs).doesNotContainKey(FormFieldId.SaveForFutureUse)
        }
    }

    @Test
    fun `absent save field preserves no preference regardless of defaults`() = runTest {
        val filter = CompleteFormFieldValueFilter(
            currentFieldValueMap = MutableStateFlow(emptyMap()),
            hiddenIdentifiers = hiddenIdentifersFlow,
            defaultValues = mapOf(FormFieldId.SaveForFutureUse to "true"),
        )

        filter.filterFlow().test {
            assertThat(awaitItem()?.userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
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
                    FormFieldId.Country to FormFieldEntry("US", true),
                    FormFieldId.Email to FormFieldEntry("email@email.com", true)
                )

            val formFieldValue = transformElementToFormFieldValueFlow.filterFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(FormFieldId.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(FormFieldId.Country)
        }
    }

    @Test
    fun `If an hidden field is incomplete field pairs have the non-hidden values`() {
        runTest {
            hiddenIdentifersFlow.value = setOf(FormFieldId.Email)

            val formFieldValues = transformElementToFormFieldValueFlow.filterFlow()

            val formFieldValue = formFieldValues.first()
            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(FormFieldId.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(FormFieldId.Country)
        }
    }

    @Test
    fun `If an hidden field is complete field pairs contain only the non-hidden values`() {
        runTest {
            fieldFlow.value =
                mapOf(
                    FormFieldId.Country to FormFieldEntry("US", true),
                    FormFieldId.Email to FormFieldEntry("email@email.com", true)
                )

            hiddenIdentifersFlow.value = setOf(emailSection.fields[0].identifier)

            val formFieldValue = transformElementToFormFieldValueFlow.filterFlow().first()

            assertThat(formFieldValue).isNotNull()
            assertThat(formFieldValue?.fieldValuePairs)
                .doesNotContainKey(FormFieldId.Email)
            assertThat(formFieldValue?.fieldValuePairs)
                .containsKey(FormFieldId.Country)
        }
    }

    @Test
    fun `Verify defaults are preserved if no fields override them`() = runTest {
        val formFieldValueFilter = CompleteFormFieldValueFilter(
            fieldFlow,
            hiddenIdentifersFlow,
            defaultValues = mapOf(
                FormFieldId.Name to "Jane Doe",
                FormFieldId.Email to "foo@bar.com",
            ),
        )

        fieldFlow.value = mapOf(
            FormFieldId.Country to FormFieldEntry("US", true),
        )

        formFieldValueFilter.filterFlow().test {
            assertThat(awaitItem()?.fieldValuePairs).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry("Jane Doe", true),
                    FormFieldId.Email to FormFieldEntry("foo@bar.com", true),
                    FormFieldId.Country to FormFieldEntry("US", true),
                )
            )
        }
    }

    @Test
    fun `Verify defaults are overridden`() = runTest {
        val formFieldValueFilter = CompleteFormFieldValueFilter(
            fieldFlow,
            hiddenIdentifersFlow,
            defaultValues = mapOf(
                FormFieldId.Name to "Jane Doe",
                FormFieldId.Email to "foo@bar.com",
            ),
        )

        fieldFlow.value = mapOf(
            FormFieldId.Country to FormFieldEntry("US", true),
            FormFieldId.Name to FormFieldEntry("Jenny Rosen", true),
            FormFieldId.Email to FormFieldEntry("mail@mail.com", true),
        )

        formFieldValueFilter.filterFlow().test {
            assertThat(awaitItem()?.fieldValuePairs).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry("Jenny Rosen", true),
                    FormFieldId.Email to FormFieldEntry("mail@mail.com", true),
                    FormFieldId.Country to FormFieldEntry("US", true),
                )
            )
        }
    }
}

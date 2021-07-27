package com.stripe.android.paymentsheet.forms

import android.content.Context
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.BankRepository
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.ResourceRepository
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Companion.NAME
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.sofort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    private val emailSection = FormItemSpec.SectionSpec(IdentifierSpec("emailSection"), Email)
    private val countrySection = FormItemSpec.SectionSpec(
        IdentifierSpec("countrySection"),
        Country()
    )

    private val resourceRepository =
        ResourceRepository(
            BankRepository(ApplicationProvider.getApplicationContext<Context>().resources),
            AddressFieldElementRepository(ApplicationProvider.getApplicationContext<Context>().resources)
        )

    @Test
    fun `Verify setting save for future use`() {
        val formViewModel = FormViewModel(
            LayoutSpec(
                listOf(
                    emailSection,
                    countrySection,
                    FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            true,
            true,
            "Example, Inc.",
            resourceRepository
        )

        val values = mutableListOf<Boolean>()
        formViewModel.saveForFutureUse.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isTrue()

        formViewModel.setSaveForFutureUse(false)

        assertThat(values[1]).isFalse()
    }

    @Test
    fun `Verify setting save for future use visibility`() {
        val formViewModel = FormViewModel(
            LayoutSpec(
                listOf(
                    emailSection,
                    countrySection,
                    FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            true,
            true,
            "Example, Inc.",
            resourceRepository
        )

        val values = mutableListOf<List<IdentifierSpec>>()
        formViewModel.hiddenIdentifiers.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isEmpty()

        formViewModel.setSaveForFutureUseVisibility(false)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(values[1][0]).isEqualTo(IdentifierSpec("save_for_future_use"))
    }

    @Test
    fun `Verify setting section as hidden sets sub-fields as hidden as well`() {
        val formViewModel = FormViewModel(
            LayoutSpec(
                listOf(
                    emailSection,
                    countrySection,
                    FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            true,
            true,
            "Example, Inc.",
            resourceRepository
        )

        val values = mutableListOf<List<IdentifierSpec>>()
        formViewModel.hiddenIdentifiers.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isEmpty()

        formViewModel.setSaveForFutureUse(false)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(values[1][0]).isEqualTo(IdentifierSpec("emailSection"))
        assertThat(values[1][1]).isEqualTo(IdentifierSpec("email"))
    }

    @Test
    fun `Verify if a field is hidden and valid it is not in the formViewValueResult`() =
        runBlocking {
            // Here we have one hidden and one required field, country will always be in the result,
            //  and name only if saveForFutureUse is true
            val formViewModel = FormViewModel(
                LayoutSpec(
                    listOf(
                        emailSection,
                        countrySection,
                        FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
                true,
                true,
                "Example, Inc.",
                resourceRepository
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController = formViewModel.elements
                .asSequence()
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .map { it.controller }
                .filterIsInstance(TextFieldController::class.java)
                .first()

            // Add text to the name to make it valid
            emailController.onValueChange("email@valid.com")

            // Verify formFieldValues contains email
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).containsKey(
                emailSection.fields[0].identifier
            )

            saveForFutureUseController.onValueChange(false)

            // Verify formFieldValues does not contain email
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
                emailSection.identifier
            )
        }

    @Test
    fun `Hidden invalid fields arent in the formViewValue and has no effect on complete state`() {
        runBlocking {
            // Here we have one hidden and one required field, country will always be in the result,
            //  and name only if saveForFutureUse is true
            val formViewModel = FormViewModel(
                LayoutSpec(
                    listOf(
                        emailSection,
                        countrySection,
                        FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
                true,
                true,
                "Example, Inc.",
                resourceRepository
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController = formViewModel.elements
                .asSequence()
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .map { it.controller }
                .filterIsInstance(TextFieldController::class.java).first()

            // Add text to the email to make it invalid
            emailController.onValueChange("email is invalid")

            // Verify formFieldValues is null because the email is required and invalid
            assertThat(formViewModel.completeFormValues.first()).isNull()

            saveForFutureUseController.onValueChange(false)

            // Verify formFieldValues is not null even though the email is invalid
            // (because it is not required)
            assertThat(formViewModel.completeFormValues.first()).isNotNull()
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
                emailSection.identifier
            )
        }
    }

    /**
     * This is serving as more of an integration test of forms from
     * spec to FormFieldValues.
     */
    @Test
    fun `Verify params are set when element flows are complete`() {
        runBlocking {
            /**
             * Using sofort as a complex enough example to test the form view model class.
             */
            val formViewModel = FormViewModel(
                sofort.layout,
                true,
                true,
                "Example, Inc.",
                resourceRepository
            )

            val nameElement = (formViewModel.elements[0] as SectionElement)
                .fields[0].controller as TextFieldController
            val emailElement = (formViewModel.elements[1] as SectionElement)
                .fields[0].controller as TextFieldController

            nameElement.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
            ).isNull()

            emailElement.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
                    ?.value
            ).isEqualTo("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
                    ?.value
            ).isEqualTo("joe")

            emailElement.onValueChange("invalid.email@IncompleteDomain")

            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
            ).isNull()
        }
    }
}

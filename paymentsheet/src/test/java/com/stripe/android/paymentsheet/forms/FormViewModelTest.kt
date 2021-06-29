package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.elements.common.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.specifications.sofort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FormViewModelTest {
    private val emailSection = FormItemSpec.SectionSpec(IdentifierSpec("emailSection"), Email)
    private val countrySection = FormItemSpec.SectionSpec(
        IdentifierSpec("countrySection"),
        Country
    )

    @Test
    fun `Verify if a field is optional and valid it is not in the formViewValueResult`() =
        runBlocking {
            // Here we have one optional and one required field, country will always be in the result,
            //  and name only if saveForFutureUse is true
            val formViewModel = FormViewModel(
                LayoutSpec(
                    listOf(
                        emailSection,
                        countrySection,
                        FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController = formViewModel.elements.map { it.controller }
                .filterIsInstance(TextFieldController::class.java).first()

            // Add text to the name to make it valid
            emailController.onValueChange("email@valid.com")

            // Verify formFieldValues contains name
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).containsKey(
                emailSection.identifier
            )

            saveForFutureUseController.onValueChange(false)

            // Verify formFieldValues does not contain name
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
                emailSection.identifier
            )
        }

    @Test
    fun `Optional invalid fields arent in the formViewValue and has no effect on complete state`() {
        runBlocking {
            // Here we have one optional and one required field, country will always be in the result,
            //  and name only if saveForFutureUse is true
            val formViewModel = FormViewModel(
                LayoutSpec(
                    listOf(
                        emailSection,
                        countrySection,
                        FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController = formViewModel.elements.map { it.controller }
                .filterIsInstance(TextFieldController::class.java).first()

            // Add text to the name to make it valid
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
            val formViewModel = FormViewModel(sofort.layout)

            val nameElement = (formViewModel.elements[0] as SectionElement)
                .field.controller as TextFieldController
            val emailElement = (formViewModel.elements[1] as SectionElement)
                .field.controller as TextFieldController

            nameElement.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Name.identifier)
            ).isNull()

            emailElement.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
            ).isEqualTo("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Name.identifier)
            ).isEqualTo("joe")

            emailElement.onValueChange("invalid.email@IncompleteDomain")

            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Name.identifier)
            ).isNull()
        }
    }
}

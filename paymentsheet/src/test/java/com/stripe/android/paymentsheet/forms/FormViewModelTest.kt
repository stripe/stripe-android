package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SectionSpec.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.specifications.sofort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FormViewModelTest {

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

            val nameElement =
                (formViewModel.elements[0] as SectionElement).field.controller as TextFieldController
            val emailElement =
                (formViewModel.elements[1] as SectionElement).field.controller as TextFieldController

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
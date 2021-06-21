package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.specifications.sofort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Using sofort as a complex enough example to test the form view model class.
 */
class FormViewModelTest {

    @Test
    fun `Verify params are set when element flows are complete`() = runBlocking {
        val formViewModel = FormViewModel(sofort.layout)

        val nameElement = formViewModel.getController(Name.identifier) as TextFieldController
        val emailElement = formViewModel.getController(Email.identifier) as TextFieldController

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
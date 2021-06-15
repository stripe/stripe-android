package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Using sofort as a complex enough example to test the form view model class.
 */
class FormViewModelTest {

    @Test
    fun `Verify params are set when element flows are complete`() = runBlocking {
        val formViewModel = FormViewModel(sofort.visualFieldLayout)

        val nameElement = formViewModel.getElement(Field.NameInput) as TextFieldElement
        val emailElement = formViewModel.getElement(Field.EmailInput) as TextFieldElement

        nameElement.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.getMap()?.get(Field.NameInput)
        ).isNull()

        emailElement.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.getMap()?.get(Field.EmailInput)
        ).isEqualTo("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.getMap()?.get(Field.NameInput)
        ).isEqualTo("joe")

        emailElement.onValueChange("invalid.email@IncompleteDomain")

        assertThat(
            formViewModel.completeFormValues.first()?.getMap()?.get(Field.NameInput)
        ).isNull()
    }
}
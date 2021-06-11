package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.DropdownElement
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
        val formViewModel = FormViewModel(sofort)

        val nameElement = formViewModel.getElement(Field.NameInput) as TextFieldElement
        val emailElement = formViewModel.getElement(Field.EmailInput) as TextFieldElement

        nameElement.onValueChange("joe")
        assertThat(formViewModel.populatedFormData.first()).isNull()

        emailElement.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.populatedFormData.first()?.toMap().toString().replace("\\s".toRegex(), "")
        ).isEqualTo(
            """
                {
                  billing_details={
                    address={
                      city=null,
                      country=US,
                      line1=null,
                      line2=null,
                      postal_code=null,
                      state=null
                    },
                    name=joe,
                    email=joe@gmail.com,
                    phone=null
                  },
                  sofort={country=US}
                }
            """.replace("\\s".toRegex(), "")
        )
        emailElement.onValueChange("invalid.email@IncompleteDomain")

        assertThat(formViewModel.populatedFormData.first()).isNull()
    }
}
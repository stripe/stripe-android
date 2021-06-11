package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
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

        val nameElement = formViewModel.getElement(Field.NameInput)
        val emailElement = formViewModel.getElement(Field.EmailInput)

        nameElement.onValueChange("joe")
        assertThat(formViewModel.paramMapFlow.first()).isNull()

        emailElement.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.paramMapFlow.first().toString().replace("\\s".toRegex(), "")
        ).isEqualTo(
            """
                {
                  billing_details={
                    address={
                      city=null,
                      country=null,
                      line1=null,
                      line2=null,
                      postal_code=null,
                      state=null
                    },
                    name=joe,
                    email=joe@gmail.com,
                    phone=null
                  },
                  sofort={country=null}
                }
            """.replace("\\s".toRegex(), "")
        )
        emailElement.onValueChange("invalid.email@IncompleteDomain")
        assertThat(formViewModel.paramMapFlow.first()).isNull()
    }
}
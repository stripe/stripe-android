package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SectionSingleFieldElement
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AddressControllerTest {
    private val emailController = TextFieldController(
        EmailConfig()
    )
    private val ibanController =
        TextFieldController(
            IbanConfig()
        )
    private val sectionFieldElementFlow = MutableStateFlow(
        listOf(
            SectionSingleFieldElement.Email(
                IdentifierSpec("email"),
                emailController
            ),
            SectionSingleFieldElement.Iban(
                IdentifierSpec("iban"),
                ibanController
            )
        )
    )
    private val addressController = AddressController(
        sectionFieldElementFlow
    )

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify the first error field is the error published`() {
        runBlocking {
            // Both email and iban invalid, should show email error first
            val emailInvalidEmailCharacters = "98kl;;;@dkc9.com"
            emailController.onValueChange(emailInvalidEmailCharacters)
            val ibanInvalidCountryCode = "12334"
            ibanController.onValueChange(ibanInvalidCountryCode)

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressController.error.first()?.errorMessage)
                .isEqualTo(R.string.email_is_invalid)

            emailController.onValueChange("joe@email.com")

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressController.error.first()?.errorMessage)
                .isEqualTo(R.string.iban_invalid_start)
        }
    }
}

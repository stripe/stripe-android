package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import com.stripe.android.uicore.R as UiCoreR

// TODO(ccen) Rewrite the test with generic Element and move it to stripe-ui-core
@RunWith(RobolectricTestRunner::class)
class AddressControllerTest {
    private val emailController = SimpleTextFieldController(
        EmailConfig()
    )
    private val ibanController =
        SimpleTextFieldController(
            IbanConfig()
        )
    private val sectionFieldElementFlow = MutableStateFlow(
        listOf(
            EmailElement(
                IdentifierSpec.Email,
                controller = emailController
            ),
            IbanElement(
                IdentifierSpec.Generic("sepa_debit[iban]"),
                ibanController
            )
        )
    )
    private val addressController = AddressController(
        sectionFieldElementFlow
    )

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
                .isEqualTo(UiCoreR.string.stripe_email_is_invalid)

            emailController.onValueChange("joe@email.com")

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressController.error.first()?.errorMessage)
                .isEqualTo(R.string.stripe_iban_invalid_start)
        }
    }
}

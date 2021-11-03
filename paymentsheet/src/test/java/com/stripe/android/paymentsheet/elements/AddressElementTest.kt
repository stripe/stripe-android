package com.stripe.android.paymentsheet.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AddressElementTest {
    private val addressFieldElementRepository = AddressFieldElementRepository(
        ApplicationProvider.getApplicationContext<Application>().resources
    )
    private val countryDropdownFieldController = DropdownFieldController(
        CountryConfig(setOf("US", "JP"))
    )

    init {
        // We want to use fields that are easy to set in error
        addressFieldElementRepository.add(
            "US",
            listOf(
                EmailElement(
                    IdentifierSpec.Email,
                    SimpleTextFieldController(EmailConfig())
                )
            )
        )
        addressFieldElementRepository.add(
            "JP",
            listOf(
                IbanElement(
                    IdentifierSpec.Generic("iban"),
                    SimpleTextFieldController(IbanConfig())
                )
            )
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify controller error is updated as the fields change based on country`() {
        runBlocking {
            // ZZ does not have state and US does
            val addressElement = AddressElement(
                IdentifierSpec.Generic("address"),
                addressFieldElementRepository,
                countryDropdownFieldController = countryDropdownFieldController
            )
            var emailController =
                (
                    (addressElement.fields.first()[1] as SectionSingleFieldElement)
                        .controller as TextFieldController
                    )

            countryDropdownFieldController.onValueChange(0)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            emailController.onValueChange(";;invalidchars@email.com")
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressElement.controller.error.first())
                .isNotNull()
            assertThat(addressElement.controller.error.first()?.errorMessage)
                .isEqualTo(R.string.email_is_invalid)

            countryDropdownFieldController.onValueChange(1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            emailController =
                (
                    (addressElement.fields.first()[1] as SectionSingleFieldElement)
                        .controller as SimpleTextFieldController
                    )
            emailController.onValueChange("12invalidiban")
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressElement.controller.error.first()?.errorMessage)
                .isEqualTo(R.string.iban_invalid_start)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `verify flow of form field values`() = runBlockingTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressFieldElementRepository,
            countryDropdownFieldController = countryDropdownFieldController
        )
        val formFieldValueFlow = addressElement.getFormFieldValueFlow()
        var emailController =
            (
                (addressElement.fields.first()[1] as SectionSingleFieldElement)
                    .controller as TextFieldController
                )

        countryDropdownFieldController.onValueChange(0)

        // Add values to the fields
        emailController.onValueChange("email")

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Verify
        var firstForFieldValues = formFieldValueFlow.first()
        assertThat(firstForFieldValues.toMap()[IdentifierSpec.Email])
            .isEqualTo(
                FormFieldEntry("email", false)
            )

        countryDropdownFieldController.onValueChange(1)

        // Add values to the fields
        emailController =
            (
                (addressElement.fields.first()[1] as SectionSingleFieldElement)
                    .controller as TextFieldController
                )
        emailController.onValueChange("DE89370400440532013000")

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        firstForFieldValues = formFieldValueFlow.first()
        assertThat(firstForFieldValues.toMap()[IdentifierSpec.Generic("iban")])
            .isEqualTo(
                FormFieldEntry("DE89370400440532013000", true)
            )
    }
}

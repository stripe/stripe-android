package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AddressElementTest {
    private val addressFieldElementRepository = AddressFieldElementRepository(mock())
    private val countryDropdownFieldController = DropdownFieldController(
        CountryConfig(setOf("US", "JP"))
    )

    init {
        // We want to use fields that are easy to set in error
        addressFieldElementRepository.add(
            "US",
            listOf(
                SectionFieldElement.Email(
                    IdentifierSpec("email"),
                    TextFieldController(EmailConfig())
                )
            )
        )
        addressFieldElementRepository.add(
            "JP",
            listOf(
                SectionFieldElement.Iban(
                    IdentifierSpec("iban"),
                    TextFieldController(IbanConfig())
                )
            )
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify controller error is updated as the fields change based on country`() {
        runBlocking {
            // ZZ does not have state and US does
            val addressElement = SectionFieldElement.AddressElement(
                IdentifierSpec("address"),
                addressFieldElementRepository,
                countryDropdownFieldController = countryDropdownFieldController
            )

            countryDropdownFieldController.onValueChange(0)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            (addressElement.fields.first()[1].controller as TextFieldController)
                .onValueChange(";;invalidchars@email.com")
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressElement.controller.error.first())
                .isNotNull()
            assertThat(addressElement.controller.error.first()?.errorMessage)
                .isEqualTo(R.string.email_is_invalid)

            countryDropdownFieldController.onValueChange(1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            ((addressElement.fields.first())[1].controller as TextFieldController)
                .onValueChange("12invalidiban")
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressElement.controller.error.first()?.errorMessage)
                .isEqualTo(R.string.iban_invalid_start)
        }
    }
}

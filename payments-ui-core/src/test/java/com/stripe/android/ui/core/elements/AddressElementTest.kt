package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AddressElementTest {
    private val addressRepository = AddressRepository(
        ApplicationProvider.getApplicationContext<Application>().resources
    )
    private val countryDropdownFieldController = DropdownFieldController(
        CountryConfig(setOf("US", "JP"))
    )

    init {
        // We want to use fields that are easy to set in error
        addressRepository.add(
            "US",
            listOf(
                EmailElement(
                    IdentifierSpec.Email,
                    controller = SimpleTextFieldController(EmailConfig())
                )
            )
        )
        addressRepository.add(
            "JP",
            listOf(
                IbanElement(
                    IdentifierSpec.Generic("sepa_debit[iban]"),
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
                addressRepository,
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
    fun `verify flow of form field values`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
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
        assertThat(firstForFieldValues.toMap()[IdentifierSpec.Generic("sepa_debit[iban]")])
            .isEqualTo(
                FormFieldEntry("DE89370400440532013000", true)
            )
    }

    @Test
    fun `condensed shipping address element should have name and phone number fields when required`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(null, PhoneNumberState.REQUIRED) { }
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Name)).isTrue()
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `hidden phone number field is not shown`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(null, PhoneNumberState.HIDDEN) { }
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isFalse()
    }

    @Test
    fun `optional phone number field is shown`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(null, PhoneNumberState.OPTIONAL) { }
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `expanded shipping address element should have name and phone number fields when required`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                PhoneNumberState.REQUIRED
            )
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Name)).isTrue()
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `expanded shipping address element should hide phone number when state is hidden`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                PhoneNumberState.HIDDEN
            )
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isFalse()
    }

    @Test
    fun `expanded shipping address element should show phone number when state is optional`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                PhoneNumberState.OPTIONAL
            )
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `normal address element should not have name and phone number fields`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.Normal()
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Name)).isFalse()
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isFalse()
    }

    @Test
    fun `normal address element should not have one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.Normal()
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isFalse()
    }

    @Test
    fun `condensed shipping address element should have one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                "some key",
                PhoneNumberState.OPTIONAL
            ) { }
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isTrue()
    }

    @Test
    fun `when google api key not supplied, condensed shipping address element is not one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                null,
                PhoneNumberState.OPTIONAL
            ) { }
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isFalse()
    }

    @Test
    fun `expanded shipping address element should not have one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            addressRepository,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                PhoneNumberState.OPTIONAL
            )
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isFalse()
    }
}

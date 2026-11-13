package com.stripe.android.paymentsheet.addresselement

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.AutocompleteAddressController
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InlineAddressFormStateTest {
    @Test
    fun `completeAddress is null without an initial address`() = runTest {
        val state = createState(initialAddress = null)

        assertThat(state.completeAddress.value).isNull()
    }

    @Test
    fun `completeAddress is null when a required field is missing`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(address = COMPLETE_ADDRESS),
        )

        assertThat(state.completeAddress.value).isNull()
    }

    @Test
    fun `completeAddress matches a complete initial address`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(
                name = "Jenny Rosen",
                address = COMPLETE_ADDRESS,
            ),
        )

        val completeAddress = state.completeAddress.value

        assertThat(completeAddress?.name).isEqualTo("Jenny Rosen")
        assertThat(completeAddress?.address).isEqualTo(COMPLETE_ADDRESS)
        assertThat(completeAddress?.phoneNumber).isNull()
    }

    @Test
    fun `an initial address outside the allowed countries is not prefilled`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(
                name = "Jenny Rosen",
                address = PaymentSheet.Address(
                    city = "Toronto",
                    country = "CA",
                    line1 = "123 Main St",
                    postalCode = "M5V 2T6",
                    state = "ON",
                ),
            ),
        )

        val enteredAddress = state.enteredAddress.value

        assertThat(enteredAddress.name).isEmpty()
        assertThat(enteredAddress.address?.country).isEqualTo("US")
        assertThat(enteredAddress.address?.line1).isEmpty()
        assertThat(state.hasChanges()).isFalse()
    }

    @Test
    fun `completeAddress keeps the initial name when the name is hidden`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(
                name = "Jenny Rosen",
                address = COMPLETE_ADDRESS,
            ),
            nameField = AddressLauncher.NameField.Never,
        )

        val completeAddress = state.completeAddress.value

        assertThat(completeAddress?.name).isEqualTo("Jenny Rosen")
        assertThat(completeAddress?.address).isEqualTo(COMPLETE_ADDRESS)
    }

    @Test
    fun `a hidden name field is not shown`() = runTest {
        val state = createState(
            initialAddress = null,
            nameField = AddressLauncher.NameField.Never,
        )

        val addressElement = state.addressController().addressElementFlow.value
        val fieldIdentifiers = addressElement.fields.value.map { it.identifier }

        assertThat(fieldIdentifiers).doesNotContain(FormFieldId.Name)
    }

    @Test
    fun `completeAddress is null when a required phone number is missing`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(
                name = "Jenny Rosen",
                address = COMPLETE_ADDRESS,
            ),
            phone = FieldConfiguration.REQUIRED,
        )

        assertThat(state.completeAddress.value).isNull()
    }

    @Test
    fun `enteredAddress includes an incomplete address`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(address = COMPLETE_ADDRESS),
        )

        val enteredAddress = state.enteredAddress.value

        assertThat(enteredAddress.name).isEmpty()
        assertThat(enteredAddress.address?.line1).isEqualTo("354 Oyster Point Blvd")
        assertThat(enteredAddress.address?.postalCode).isEqualTo("94080")
    }

    @Test
    fun `enteredAddress follows input changes`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(address = COMPLETE_ADDRESS),
        )

        state.formController.setRawValues(mapOf(FormFieldId.Name to "Jenny Rosen"))

        assertThat(state.enteredAddress.value.name).isEqualTo("Jenny Rosen")
    }

    @Test
    fun `hasChanges is false before input changes`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(
                name = "Jenny Rosen",
                address = COMPLETE_ADDRESS,
            ),
        )

        assertThat(state.hasChanges()).isFalse()
    }

    @Test
    fun `hasChanges is true after input changes`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(
                name = "Jenny Rosen",
                address = COMPLETE_ADDRESS,
            ),
        )

        state.formController.setRawValues(mapOf(FormFieldId.Name to "John Doe"))

        assertThat(state.hasChanges()).isTrue()
    }

    @Test
    fun `validate shows an error on an incomplete form`() = runTest {
        val state = createState(initialAddress = null)
        val addressController = state.addressController()
        assertThat(addressController.validationMessage.value).isNull()

        state.validate()

        assertThat(addressController.validationMessage.value).isNotNull()
    }

    @Test
    fun `an empty form without a publishable key starts with every field shown`() = runTest {
        val state = createState(initialAddress = null)

        val addressElement = state.addressController().addressElementFlow.value

        assertThat(addressElement.addressInputMode)
            .isInstanceOf(AddressInputMode.NoAutocomplete::class.java)
    }

    @Test
    fun `an empty form with a publishable key starts with inline autocomplete`() = runTest {
        val state = createState(
            initialAddress = null,
            publishableKey = "pk_test_123",
        )

        val addressElement = state.addressController().addressElementFlow.value

        assertThat(addressElement.addressInputMode)
            .isInstanceOf(AddressInputMode.AutocompleteInline::class.java)
    }

    @Test
    fun `a prefilled form with a publishable key starts with every field shown`() = runTest {
        val state = createState(
            initialAddress = AddressDetails(
                name = "Jenny Rosen",
                address = COMPLETE_ADDRESS,
            ),
            publishableKey = "pk_test_123",
        )

        val addressElement = state.addressController().addressElementFlow.value

        assertThat(addressElement.addressInputMode)
            .isInstanceOf(AddressInputMode.NoAutocomplete::class.java)
    }

    private fun TestScope.createState(
        initialAddress: AddressDetails?,
        nameField: AddressLauncher.NameField = AddressLauncher.NameField.Auto,
        phone: FieldConfiguration = FieldConfiguration.HIDDEN,
        publishableKey: String? = null,
    ): InlineAddressFormState {
        return InlineAddressFormState(
            initialAddress = initialAddress,
            allowedCountries = setOf("US"),
            nameField = nameField,
            phone = phone,
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = publishableKey,
            coroutineScope = backgroundScope,
        )
    }

    private fun InlineAddressFormState.addressController(): AutocompleteAddressController {
        val section = formController.elements.single() as SectionElement
        return (section.fields.single() as AutocompleteAddressElement).sectionFieldErrorController()
    }

    private companion object {
        val COMPLETE_ADDRESS = PaymentSheet.Address(
            city = "South San Francisco",
            country = "US",
            line1 = "354 Oyster Point Blvd",
            line2 = "Suite 100",
            postalCode = "94080",
            state = "CA",
        )
    }
}

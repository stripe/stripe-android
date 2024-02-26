package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class PhoneNumberControllerTest {

    @Test
    fun `when new country is selected then phoneNumberFormatter is updated`() = runTest {
        val phoneNumberController = PhoneNumberController(
            initiallySelectedCountryCode = "US",
            overrideCountryCodes = setOf("US", "BR"),
            workContext = UnconfinedTestDispatcher(),
        )

        turbineScope {
            val rawFieldValue = phoneNumberController.rawFieldValue.testIn(this)
            val fieldValue = phoneNumberController.fieldValue.testIn(this)
            val transformation = phoneNumberController.visualTransformation.testIn(this)

            val usTransformation = transformation.awaitItem()

            assertThat(rawFieldValue.awaitItem()).isEqualTo("+1")
            assertThat(fieldValue.awaitItem()).isEqualTo("")

            phoneNumberController.onValueChange("1234567890")

            assertThat(rawFieldValue.awaitItem()).isEqualTo("+11234567890")

            val currentFieldValue = fieldValue.awaitItem()

            assertThat(currentFieldValue).isEqualTo("1234567890")
            assertThat(usTransformation.filter(AnnotatedString(currentFieldValue)).text.text)
                .isEqualTo("(123) 456-7890")

            phoneNumberController.countryDropdownController.onValueChange(1)

            assertThat(rawFieldValue.awaitItem()).isEqualTo("+551234567890")

            val brTransformation = transformation.awaitItem()

            assertThat(brTransformation.filter(AnnotatedString(currentFieldValue)).text.text)
                .isEqualTo("12 34567-890")

            rawFieldValue.cancel()
            fieldValue.cancel()
            transformation.cancel()
        }
    }

    @Test
    fun `when any number was input then isComplete is true`() = runTest {
        val phoneNumberController = PhoneNumberController(
            workContext = UnconfinedTestDispatcher(),
        )

        phoneNumberController.isComplete.test {
            assertThat(awaitItem()).isFalse()

            phoneNumberController.onValueChange("1")
            idleLooper()
            assertThat(awaitItem()).isFalse()

            phoneNumberController.onValueChange("")
            idleLooper()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `any input is marked complete if field is optional`() = runTest {
        val phoneNumberController = PhoneNumberController(
            showOptionalLabel = true,
            acceptAnyInput = true,
            workContext = UnconfinedTestDispatcher(),
        )

        phoneNumberController.isComplete.test {
            assertThat(awaitItem()).isTrue()

            phoneNumberController.onValueChange("1")
            idleLooper()
            assertThat(awaitItem()).isTrue()

            phoneNumberController.onValueChange("")
            idleLooper()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `when initial number is in E164 format then initial country is set`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+491234567890",
            workContext = UnconfinedTestDispatcher(),
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("DE")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    fun `when initial country is set then prefix is removed from initial number`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+441234567890",
            initiallySelectedCountryCode = "JE",
            workContext = UnconfinedTestDispatcher(),
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("JE")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    @Config(qualifiers = "fr-rCA")
    fun `when initial number is in E164 format with multiple regions then locale is used`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+11234567890",
            workContext = UnconfinedTestDispatcher(),
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("CA")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    @Config(qualifiers = "fr-rCA")
    fun `when initial number is not in E164 format then locale is used`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "1234567890",
            workContext = UnconfinedTestDispatcher(),
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("CA")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    fun `when phone number is less than expected length error is emitted`() = runTest {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initiallySelectedCountryCode = "US",
            workContext = UnconfinedTestDispatcher(),
        )

        turbineScope {
            val isComplete = phoneNumberController.isComplete.testIn(backgroundScope)

            val error = phoneNumberController.error.testIn(backgroundScope)

            assertThat(isComplete.awaitItem()).isFalse()
            assertThat(error.awaitItem()).isNull()

            isComplete.ensureAllEventsConsumed()
            error.ensureAllEventsConsumed()

            phoneNumberController.onValueChange("1")
            idleLooper()
            assertThat(isComplete.awaitItem()).isFalse()
            error.skipItems(1)
            assertThat(error.awaitItem()).isNotNull()

            isComplete.ensureAllEventsConsumed()
            error.ensureAllEventsConsumed()

            phoneNumberController.onValueChange("1234567891")
            idleLooper()
            assertThat(isComplete.awaitItem()).isTrue()
            error.skipItems(1)
            assertThat(error.awaitItem()).isNull()

            isComplete.ensureAllEventsConsumed()
            error.ensureAllEventsConsumed()
        }
    }
}

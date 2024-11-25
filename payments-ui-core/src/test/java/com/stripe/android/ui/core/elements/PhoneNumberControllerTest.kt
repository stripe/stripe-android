package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.PhoneNumberController
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class PhoneNumberControllerTest {

    @Test
    fun `when new country is selected then phoneNumberFormatter is updated`() = runTest {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initiallySelectedCountryCode = "US",
            overrideCountryCodes = setOf("US", "BR"),
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
    fun `incomplete input is marked correctly if field is not optional`() = runTest {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController()

        phoneNumberController.isComplete.test {
            assertThat(awaitItem()).isFalse()

            phoneNumberController.onValueChange("1")
            expectNoEvents()

            phoneNumberController.onValueChange("")
            expectNoEvents()
        }
    }

    @Test
    fun `any input is marked complete if field is optional`() = runTest {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            showOptionalLabel = true,
            acceptAnyInput = true,
        )

        phoneNumberController.isComplete.test {
            assertThat(awaitItem()).isTrue()

            phoneNumberController.onValueChange("1")
            expectNoEvents()

            phoneNumberController.onValueChange("")
            expectNoEvents()
        }
    }

    @Test
    fun `when initial number is in E164 format then initial country is set`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+491234567890",
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("DE")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    fun `when initial country is set then prefix is removed from initial number`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+441234567890",
            initiallySelectedCountryCode = "JE",
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("JE")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    @Config(qualifiers = "fr-rCA")
    fun `when initial number is in E164 format with multiple regions then locale is used`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+11234567890",
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("CA")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    @Config(qualifiers = "fr-rCA")
    fun `when initial number is not in E164 format then locale is used`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "1234567890",
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("CA")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    fun `when phone number is less than expected length error is emitted`() = runTest {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initiallySelectedCountryCode = "US",
        )

        phoneNumberController.error.test {
            assertThat(awaitItem()).isNull()

            phoneNumberController.onValueChange("1")
            assertThat(awaitItem()).isNotNull()

            phoneNumberController.onValueChange("1234567891")
            skipItems(1)

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `when phone number is less than expected length field is not considered complete`() = runTest {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initiallySelectedCountryCode = "US",
        )

        phoneNumberController.isComplete.test {
            assertThat(awaitItem()).isFalse()

            phoneNumberController.onValueChange("1")
            expectNoEvents()

            phoneNumberController.onValueChange("1234567891")
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `when phone number is entered, form field value should contain prefix`() = runTest {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initiallySelectedCountryCode = "CA",
        )

        phoneNumberController.onValueChange("(122) 252-5252")

        phoneNumberController.formFieldValue.test {
            assertThat(awaitItem().value).isEqualTo("+11222525252")
        }
    }
}

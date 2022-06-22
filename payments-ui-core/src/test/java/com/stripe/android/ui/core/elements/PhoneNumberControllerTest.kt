package com.stripe.android.ui.core.elements

import androidx.lifecycle.asLiveData
import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class PhoneNumberControllerTest {

    @Test
    fun `when new country is selected then phoneNumberFormatter is updated`() {
        val phoneNumberController = PhoneNumberController(
            initiallySelectedCountryCode = "US",
            overrideCountryCodes = setOf("US", "BR")
        )

        val rawValue = mutableListOf<String>()
        phoneNumberController.rawFieldValue.asLiveData().observeForever {
            rawValue.add(it)
        }
        phoneNumberController.onValueChange("1234567890")
        idleLooper()
        assertThat(rawValue.last()).isEqualTo("+11234567890")

        phoneNumberController.onSelectedCountryIndex(1)
        idleLooper()
        // Input value remains the same, but now with country code +55
        assertThat(rawValue.last()).isEqualTo("+551234567890")
    }

    @Test
    fun `when any number was input then isComplete is true`() {
        val phoneNumberController = PhoneNumberController()

        val isComplete = mutableListOf<Boolean>()
        phoneNumberController.isComplete.asLiveData().observeForever {
            isComplete.add(it)
        }
        assertThat(isComplete.last()).isFalse()

        phoneNumberController.onValueChange("1")
        idleLooper()
        assertThat(isComplete.last()).isTrue()

        phoneNumberController.onValueChange("")
        idleLooper()
        assertThat(isComplete.last()).isFalse()
    }

    @Test
    fun `when initial number is in E164 format then initial country is set`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+491234567890"
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("DE")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    fun `when initial country is set then prefix is removed from initial number`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+441234567890",
            initiallySelectedCountryCode = "JE"
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("JE")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    @Config(qualifiers = "fr-rCA")
    fun `when initial number is in E164 format with multiple regions then locale is used`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+11234567890"
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("CA")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }

    @Test
    @Config(qualifiers = "fr-rCA")
    fun `when initial number is not in E164 format then locale is used`() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "1234567890"
        )

        assertThat(phoneNumberController.getCountryCode()).isEqualTo("CA")
        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo("1234567890")
    }
}

package com.stripe.android.ui.core.elements

import androidx.lifecycle.asLiveData
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OTPControllerTest {
    private val otpConfig = OTPController(1)

    @Test
    fun `verify only numbers are allowed`() {
        val emittedValues = mutableListOf<String>()
        otpConfig.fieldValues.forEach { flow ->
            flow.asLiveData().observeForever {
                emittedValues.add(it)
            }
        }

        otpConfig.onValueChanged(0, "stripe")

        assertThat(emittedValues).isEmpty()
    }

//    @Test
//    fun `verify blank otp cell returns blank state`() {
//        Truth.assertThat(otpConfig.determineState(""))
//            .isEqualTo(TextFieldStateConstants.Error.Blank)
//    }
//
//    @Test
//    fun `verify valid BSB is in valid state`() {
//        Truth.assertThat(otpConfig.determineState("1"))
//            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
//        Truth.assertThat(otpConfig.determineState("12"))
//            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
//    }
}

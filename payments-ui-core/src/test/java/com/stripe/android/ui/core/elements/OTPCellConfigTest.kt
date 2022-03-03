package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth
import org.junit.Test

class OTPCellConfigTest {
    private val otpConfig = OTPCellConfig()

    @Test
    fun `verify only numbers are allowed in the field`() {
        Truth.assertThat(otpConfig.filter("g"))
            .isEqualTo("")
    }

    @Test
    fun `verify blank otp cell returns blank state`() {
        Truth.assertThat(otpConfig.determineState(""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `verify valid BSB is in valid state`() {
        Truth.assertThat(otpConfig.determineState("1"))
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
        Truth.assertThat(otpConfig.determineState("12"))
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}

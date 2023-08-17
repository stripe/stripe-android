package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants
import org.junit.Test

class AuBankAccountNumberConfigTest {
    private val auBankAccountNumberConfig = AuBankAccountNumberConfig()

    @Test
    fun `verify only numbers are allowed in the field`() {
        assertThat(auBankAccountNumberConfig.filter("12345hell6789"))
            .isEqualTo("123456789")
    }

    @Test
    fun `verify limits input to accepted length`() {
        assertThat(auBankAccountNumberConfig.filter("1234567899999")).isEqualTo("123456789")
    }

    @Test
    fun `verify blank bank account number returns blank state`() {
        assertThat(auBankAccountNumberConfig.determineState(""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `verify incomplete bank account number is in incomplete state`() {
        assertThat(auBankAccountNumberConfig.determineState("123"))
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)

        assertThat(auBankAccountNumberConfig.determineState("123456"))
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
    }

    @Test
    fun `verify valid bank account number is in valid state`() {
        assertThat(auBankAccountNumberConfig.determineState("653987643"))
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}

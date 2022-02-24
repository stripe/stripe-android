package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth
import org.junit.Test

class AuBankAccountNumberConfigTest {
    private val auBankAccountNumberConfig = AuBankAccountNumberConfig()

    @Test
    fun `verify only numbers are allowed in the field`() {
        Truth.assertThat(auBankAccountNumberConfig.filter("12345hell6789"))
            .isEqualTo("123456789")
    }

    @Test
    fun `verify blank bank account number returns blank state`() {
        Truth.assertThat(auBankAccountNumberConfig.determineState(""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `verify incomplete bank account number is in incomplete state`() {
        Truth.assertThat(auBankAccountNumberConfig.determineState("123"))
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)

        Truth.assertThat(auBankAccountNumberConfig.determineState("123456"))
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
    }

    @Test
    fun `verify invalid bank account number is in invalid state`() {
        Truth.assertThat(auBankAccountNumberConfig.determineState("1234567890"))
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
    }

    @Test
    fun `verify valid bank account number is in valid state`() {
        Truth.assertThat(auBankAccountNumberConfig.determineState("653987643"))
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}

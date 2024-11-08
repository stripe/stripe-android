package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.utils.isInstanceOf
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
        assertThat(auBankAccountNumberConfig.determineState("12"))
            .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()

        assertThat(auBankAccountNumberConfig.determineState("123"))
            .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
    }

    @Test
    fun `verify valid bank account number but not maximum is in limitless state`() {
        assertThat(auBankAccountNumberConfig.determineState("1234"))
            .isInstanceOf<TextFieldStateConstants.Valid.Limitless>()

        assertThat(auBankAccountNumberConfig.determineState("12345"))
            .isInstanceOf<TextFieldStateConstants.Valid.Limitless>()

        assertThat(auBankAccountNumberConfig.determineState("123456"))
            .isInstanceOf<TextFieldStateConstants.Valid.Limitless>()

        assertThat(auBankAccountNumberConfig.determineState("1234567"))
            .isInstanceOf<TextFieldStateConstants.Valid.Limitless>()

        assertThat(auBankAccountNumberConfig.determineState("12345678"))
            .isInstanceOf<TextFieldStateConstants.Valid.Limitless>()
    }

    @Test
    fun `verify valid bank account number is in valid state`() {
        assertThat(auBankAccountNumberConfig.determineState("653987643"))
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }
}

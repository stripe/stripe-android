package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants
import org.junit.Test

class BacsDebitAccountNumberConfigTest {
    private val bacsDebitAccountNumberConfig = BacsDebitAccountNumberConfig()

    @Test
    fun `verify config uses proper visual transformation, keyboard capitalization, and keyboard type`() {
        assertThat(
            bacsDebitAccountNumberConfig.visualTransformation
        ).isEqualTo(VisualTransformation.None)

        assertThat(
            bacsDebitAccountNumberConfig.capitalization
        ).isEqualTo(KeyboardCapitalization.None)

        assertThat(
            bacsDebitAccountNumberConfig.keyboard
        ).isEqualTo(KeyboardType.NumberPassword)
    }

    @Test
    fun `verify only numbers are allowed in the field`() {
        assertThat(
            bacsDebitAccountNumberConfig.filter("12345ewgh6789")
        ).isEqualTo("12345678")
    }

    @Test
    fun `verify limits input to accepted length`() {
        assertThat(
            bacsDebitAccountNumberConfig.filter("1234567899999")
        ).isEqualTo("12345678")
    }

    @Test
    fun `verify blank account number returns blank state`() {
        assertThat(
            bacsDebitAccountNumberConfig.determineState("")
        ).isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `verify incomplete account number is in incomplete state`() {
        assertThat(
            bacsDebitAccountNumberConfig.determineState("123")
        ).isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)

        assertThat(
            bacsDebitAccountNumberConfig.determineState("123456")
        ).isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
    }

    @Test
    fun `verify valid account number is in valid state`() {
        assertThat(
            bacsDebitAccountNumberConfig.determineState("65398764")
        ).isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}

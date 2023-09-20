package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants
import org.junit.Test

class BacsDebitSortCodeConfigTest {
    private val bacsDebitSortCodeConfig = BacsDebitSortCodeConfig()

    @Test
    fun `verify config uses proper visual transformation, keyboard capitalization, and keyboard type`() {
        assertThat(
            bacsDebitSortCodeConfig.visualTransformation
        ).isEqualTo(BacsDebitSortCodeVisualTransformation)

        assertThat(
            bacsDebitSortCodeConfig.capitalization
        ).isEqualTo(KeyboardCapitalization.None)

        assertThat(
            bacsDebitSortCodeConfig.keyboard
        ).isEqualTo(KeyboardType.NumberPassword)
    }

    @Test
    fun `verify only numbers are allowed in the field`() {
        assertThat(
            bacsDebitSortCodeConfig.filter("12345hell6789")
        ).isEqualTo("123456")
    }

    @Test
    fun `verify limits input to accepted length`() {
        assertThat(
            bacsDebitSortCodeConfig.filter("1234567899999")
        ).isEqualTo("123456")
    }

    @Test
    fun `verify blank sort code returns blank state`() {
        assertThat(
            bacsDebitSortCodeConfig.determineState("")
        ).isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `verify incomplete sort code is in incomplete state`() {
        assertThat(
            bacsDebitSortCodeConfig.determineState("123")
        ).isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)

        assertThat(
            bacsDebitSortCodeConfig.determineState("12345")
        ).isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
    }

    @Test
    fun `verify valid sort code is in valid state`() {
        assertThat(
            bacsDebitSortCodeConfig.determineState("653987")
        ).isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}

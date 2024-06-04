package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.view.BecsDebitBanks
import org.junit.Test

class BsbConfigTest {
    private val banks: List<BecsDebitBanks.Bank> = listOf(
        BecsDebitBanks.Bank("00", "Stripe Test Bank")
    )
    private val bsbConfig = BsbConfig(banks)

    @Test
    fun `visualTransformation formats entered value`() {
        Truth.assertThat(bsbConfig.visualTransformation.filter(AnnotatedString("000000")).text)
            .isEqualTo(AnnotatedString("000 - 000"))
    }

    @Test
    fun `verify visualTransformation formats entered value`() {
        Truth.assertThat(bsbConfig.visualTransformation.filter(AnnotatedString("123456")).text)
            .isEqualTo(AnnotatedString("123 - 456"))
    }

    @Test
    fun `verify only numbers are allowed in the field`() {
        Truth.assertThat(bsbConfig.filter("123h456"))
            .isEqualTo("123456")
    }

    @Test
    fun `verify blank BSB returns blank state`() {
        Truth.assertThat(bsbConfig.determineState(""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `verify incomplete BSB is in incomplete state`() {
        Truth.assertThat(bsbConfig.determineState("123"))
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)

        Truth.assertThat(bsbConfig.determineState("12345"))
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
    }

    @Test
    fun `verify invalid BSB is in invalid state`() {
        // input length > LENGTH
        Truth.assertThat(bsbConfig.determineState("1234567"))
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)

        // invalid BSB prefix
        Truth.assertThat(bsbConfig.determineState("891200"))
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
    }

    @Test
    fun `verify valid BSB is in valid state`() {
        Truth.assertThat(bsbConfig.determineState("001234"))
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}

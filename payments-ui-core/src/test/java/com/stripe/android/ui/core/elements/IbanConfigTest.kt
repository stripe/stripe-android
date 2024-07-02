package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.utils.isInstanceOf
import org.junit.Test

class IbanConfigTest {
    private val ibanConfig = IbanConfig()

    @Test
    fun `visualTransformation formats entered value`() {
        assertThat(ibanConfig.visualTransformation.filter(AnnotatedString("12345678901234")).text)
            .isEqualTo(AnnotatedString("1234 5678 9012 34"))
    }

    @Test
    fun `only letters and numbers are allowed in the field`() {
        assertThat(ibanConfig.filter("123^@IBan[\uD83E\uDD57."))
            .isEqualTo("123IBAN")
    }

    @Test
    fun `blank IBAN returns blank state`() {
        assertThat(ibanConfig.determineState(""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `IBAN must start with valid country code`() {
        assertThat(ibanConfig.determineState("123ABC"))
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()

        assertThat(ibanConfig.determineState("XX3ABC"))
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
    }

    @Test
    fun `incomplete IBAN is in incomplete state`() {
        assertThat(ibanConfig.determineState("A"))
            .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()

        assertThat(ibanConfig.determineState("CA3ABC"))
            .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()

        assertThat(ibanConfig.determineState("NL38RABO0300065264"))
            .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
    }

    @Test
    fun `valid IBAN is in valid state`() {
        assertThat(ibanConfig.determineState("NL39RABO0300065264"))
            .isInstanceOf<TextFieldStateConstants.Valid.Limitless>()

        assertThat(ibanConfig.determineState("AT41190430023457320104300234573201"))
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `invalid IBAN is in invalid state`() {
        // invalid country
        assertThat(ibanConfig.determineState("ABCD61190430023457320"))
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()

        // starts with digits
        assertThat(ibanConfig.determineState("11CD61190430023457320"))
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
    }
}

package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants
import org.junit.Test

internal class PixTaxIdConfigTest {
    private val config = PixTaxIdConfig()

    @Test
    fun `filter keeps at most fourteen digits`() {
        assertThat(config.filter("12.345.678/9012-345١"))
            .isEqualTo("12345678901234")
    }

    @Test
    fun `eleven digits is a valid CPF and permits more input`() {
        val state = config.determineState("12345678901")

        assertThat(state).isInstanceOf(TextFieldStateConstants.Valid.Limitless::class.java)
    }

    @Test
    fun `fourteen digits is a valid and full CNPJ`() {
        val state = config.determineState("12345678901234")

        assertThat(state).isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }

    @Test
    fun `other lengths are invalid`() {
        assertThat(config.determineState("").isValid()).isFalse()
        assertThat(config.determineState("1234567890").isValid()).isFalse()
        assertThat(config.determineState("123456789012").isValid()).isFalse()
    }

    @Test
    fun `visual transformation formats CPF`() {
        val transformed = config.visualTransformation.filter(AnnotatedString("12345678901"))

        assertThat(transformed.text.text).isEqualTo("123.456.789-01")
    }

    @Test
    fun `visual transformation formats CNPJ`() {
        val transformed = config.visualTransformation.filter(AnnotatedString("12345678901234"))

        assertThat(transformed.text.text).isEqualTo("12.345.678/9012-34")
    }
}

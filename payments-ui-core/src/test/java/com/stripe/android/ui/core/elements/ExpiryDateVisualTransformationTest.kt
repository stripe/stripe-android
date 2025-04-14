package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.ExpiryDateVisualTransformation
import org.junit.Test

internal class ExpiryDateVisualTransformationTest {

    private val transform = ExpiryDateVisualTransformation()

    @Test
    fun `verify 19 get separated between 1 and 9`() {
        val result = transform.filter(AnnotatedString("19"))
        assertThat(result.text.text).isEqualTo("1 / 9")
        assertCorrectMapping(original = "19", result)
    }

    @Test
    fun `verify 123 get separated between 2 and 3`() {
        val result = transform.filter(AnnotatedString("123"))
        assertThat(result.text.text).isEqualTo("12 / 3")
        assertCorrectMapping(original = "123", result)
    }

    @Test
    fun `verify 143 get separated between 1 and 4`() {
        val result = transform.filter(AnnotatedString("143"))
        assertThat(result.text.text).isEqualTo("1 / 43")
        assertCorrectMapping(original = "143", result)
    }

    @Test
    fun `verify 093 get separated between 9 and 3`() {
        val result = transform.filter(AnnotatedString("093"))
        assertThat(result.text.text).isEqualTo("09 / 3")
        assertCorrectMapping(original = "093", result)
    }

    @Test
    fun `verify 53 get separated between 5 and 3`() {
        val result = transform.filter(AnnotatedString("53"))
        assertThat(result.text.text).isEqualTo("5 / 3")
        assertCorrectMapping(original = "53", result)
    }

    @Test
    fun `transformation is ignored for fallbackExpiryDate`() {
        val transform = ExpiryDateVisualTransformation(fallbackExpiryDate = "•• / ••")
        val result = transform.filter(AnnotatedString("•• / ••"))
        assertThat(result.text.text).isEqualTo("•• / ••")
    }

    private fun assertCorrectMapping(
        original: String,
        result: TransformedText,
    ) {
        val transformed = result.text.text

        for (offset in 0..original.length) {
            val transformedOffset = result.offsetMapping.originalToTransformed(offset)
            assertThat(transformedOffset).isIn(0..transformed.length)
        }

        for (offset in 0..result.text.text.length) {
            val originalOffset = result.offsetMapping.transformedToOriginal(offset)
            assertThat(originalOffset).isIn(0..original.length)
        }
    }
}

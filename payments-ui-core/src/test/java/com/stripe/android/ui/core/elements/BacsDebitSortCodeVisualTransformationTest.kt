package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BacsDebitSortCodeVisualTransformationTest {
    private val transformation = BacsDebitSortCodeVisualTransformation

    @Test
    fun `on no characters, should format & offset properly`() {
        val transformedText = transformation.filter(AnnotatedString(""))

        assertThat(transformedText.text.text).isEqualTo("")

        assertThat(
            transformedText.offsetMapping.originalToTransformed(offset = 0)
        ).isEqualTo(0)

        assertThat(
            transformedText.offsetMapping.transformedToOriginal(offset = 0)
        ).isEqualTo(0)
    }

    @Test
    fun `on 1 character, should format & offset properly`() {
        val transformedText = transformation.filter(AnnotatedString("1"))

        assertThat(transformedText.text.text).isEqualTo("1")

        assertThat(
            transformedText.offsetMapping.originalToTransformed(offset = 1)
        ).isEqualTo(1)

        assertThat(
            transformedText.offsetMapping.transformedToOriginal(offset = 1)
        ).isEqualTo(1)
    }

    @Test
    fun `on 2 characters, should format & offset properly`() {
        val transformedText = transformation.filter(AnnotatedString("12"))

        assertThat(transformedText.text.text).isEqualTo("12")

        assertThat(
            transformedText.offsetMapping.originalToTransformed(offset = 2)
        ).isEqualTo(2)

        assertThat(
            transformedText.offsetMapping.transformedToOriginal(offset = 2)
        ).isEqualTo(2)
    }

    @Test
    fun `on 3 characters, should format & offset properly`() {
        val transformedText = transformation.filter(AnnotatedString("123"))

        assertThat(transformedText.text.text).isEqualTo("12-3")

        assertThat(
            transformedText.offsetMapping.originalToTransformed(offset = 3)
        ).isEqualTo(4)

        assertThat(
            transformedText.offsetMapping.transformedToOriginal(offset = 4)
        ).isEqualTo(3)
    }

    @Test
    fun `on 4 characters, should format & offset properly`() {
        val transformedText = transformation.filter(AnnotatedString("1234"))

        assertThat(transformedText.text.text).isEqualTo("12-34")

        assertThat(
            transformedText.offsetMapping.originalToTransformed(offset = 4)
        ).isEqualTo(5)

        assertThat(
            transformedText.offsetMapping.transformedToOriginal(offset = 5)
        ).isEqualTo(4)
    }

    @Test
    fun `on 5 characters, should format & offset properly`() {
        val transformedText = transformation.filter(AnnotatedString("12345"))

        assertThat(transformedText.text.text).isEqualTo("12-34-5")

        assertThat(
            transformedText.offsetMapping.originalToTransformed(offset = 5)
        ).isEqualTo(7)

        assertThat(
            transformedText.offsetMapping.transformedToOriginal(offset = 7)
        ).isEqualTo(5)
    }

    @Test
    fun `on 6 characters, should format & offset properly`() {
        val transformedText = transformation.filter(AnnotatedString("123456"))

        assertThat(transformedText.text.text).isEqualTo("12-34-56")

        assertThat(
            transformedText.offsetMapping.originalToTransformed(offset = 6)
        ).isEqualTo(8)

        assertThat(
            transformedText.offsetMapping.transformedToOriginal(offset = 8)
        ).isEqualTo(6)
    }
}

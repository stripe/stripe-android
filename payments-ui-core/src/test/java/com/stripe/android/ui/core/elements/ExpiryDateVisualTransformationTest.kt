package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth
import org.junit.Test

internal class ExpiryDateVisualTransformationTest {
    private val transform = ExpiryDateVisualTransformation()

    @Test
    fun `verify 19 get separated between 1 and 9`() {
        Truth.assertThat(transform.filter(AnnotatedString("19")).text.text)
            .isEqualTo("1 / 9")
    }

    @Test
    fun `verify 123 get separated between 2 and 3`() {
        Truth.assertThat(transform.filter(AnnotatedString("123")).text.text)
            .isEqualTo("12 / 3")
    }

    @Test
    fun `verify 093 get separated between 9 and 3`() {
        Truth.assertThat(transform.filter(AnnotatedString("093")).text.text)
            .isEqualTo("09 / 3")
    }

    @Test
    fun `verify 53 get separated between 5 and 3`() {
        Truth.assertThat(transform.filter(AnnotatedString("53")).text.text)
            .isEqualTo("5 / 3")
    }
}

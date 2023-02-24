package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth
import com.stripe.android.uicore.elements.PostalCodeConfig
import com.stripe.android.uicore.elements.PostalCodeVisualTransformation
import org.junit.Test

internal class PostalCodeVisualTransformationTest {

    @Test
    fun `verify US zip`() {
        val transform = PostalCodeVisualTransformation(
            PostalCodeConfig.CountryPostalFormat.US
        )
        Truth.assertThat(transform.filter(AnnotatedString("90210")).text.text)
            .isEqualTo("90210")
    }

    @Test
    fun `verify CA postal`() {
        val transform = PostalCodeVisualTransformation(
            PostalCodeConfig.CountryPostalFormat.CA
        )
        Truth.assertThat(transform.filter(AnnotatedString("A0A0A0")).text.text)
            .isEqualTo("A0A 0A0")
    }

    @Test
    fun `verify CA postal with less than 6`() {
        val transform = PostalCodeVisualTransformation(
            PostalCodeConfig.CountryPostalFormat.CA
        )
        Truth.assertThat(transform.filter(AnnotatedString("A0A0A")).text.text)
            .isEqualTo("A0A 0A")
    }

    @Test
    fun `verify CA postal with lower`() {
        val transform = PostalCodeVisualTransformation(
            PostalCodeConfig.CountryPostalFormat.CA
        )
        Truth.assertThat(transform.filter(AnnotatedString("a0a0a0")).text.text)
            .isEqualTo("A0A 0A0")
    }
}

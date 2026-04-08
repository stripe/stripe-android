package com.stripe.android.identity.ui

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IdentityThemeTest {
    @Test
    fun `withBrandColor returns original colors when brandColor is null`() {
        val baseColors = lightColors()

        val resolvedColors = baseColors.withBrandColor(null)

        assertThat(resolvedColors).isSameInstanceAs(baseColors)
    }

    @Test
    fun `withBrandColor updates primary colors only`() {
        val baseColors = lightColors(
            primary = Color.Red,
            primaryVariant = Color.Blue,
            secondary = Color.Green,
            secondaryVariant = Color.Yellow,
        )

        val resolvedColors = baseColors.withBrandColor(Color.Magenta.toArgb())

        assertThat(resolvedColors.primary.toArgb()).isEqualTo(Color.Magenta.toArgb())
        assertThat(resolvedColors.primaryVariant.toArgb()).isEqualTo(Color.Blue.toArgb())
        assertThat(resolvedColors.secondary.toArgb()).isEqualTo(Color.Green.toArgb())
        assertThat(resolvedColors.secondaryVariant.toArgb()).isEqualTo(Color.Yellow.toArgb())
    }

    @Test
    fun `withBrandColor uses light foreground for dark colors`() {
        val resolvedColors = darkColors().withBrandColor(Color.Black.toArgb())

        assertThat(resolvedColors.onPrimary.toArgb()).isEqualTo(Color.White.toArgb())
    }

    @Test
    fun `withBrandColor uses dark foreground for light colors`() {
        val resolvedColors = lightColors().withBrandColor(Color.White.toArgb())

        assertThat(resolvedColors.onPrimary.toArgb()).isEqualTo(Color.Black.toArgb())
    }
}

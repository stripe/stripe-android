package com.stripe.android.common.taptoadd.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.stripeterminal.external.InternalApi
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import org.junit.Test

@OptIn(AppearanceAPIAdditionsPreview::class, InternalApi::class)
internal class TapToAddThemeTest {
    @Test
    fun `automatic uses light terminal appearance in system light`() {
        val configuration = createTapToAddUxConfiguration(
            appearance = createAppearance(PaymentSheet.ThemeMode.Automatic),
            isSystemDark = false,
        )

        assertConfiguration(
            configuration = configuration,
            expectedDarkMode = TapToPayUxConfiguration.DarkMode.LIGHT,
            expectedPrimary = LIGHT_PRIMARY,
        )
    }

    @Test
    fun `automatic uses dark terminal appearance in system dark`() {
        val configuration = createTapToAddUxConfiguration(
            appearance = createAppearance(PaymentSheet.ThemeMode.Automatic),
            isSystemDark = true,
        )

        assertConfiguration(
            configuration = configuration,
            expectedDarkMode = TapToPayUxConfiguration.DarkMode.DARK,
            expectedPrimary = DARK_PRIMARY,
        )
    }

    @Test
    fun `always light uses light terminal appearance in system dark`() {
        val configuration = createTapToAddUxConfiguration(
            appearance = createAppearance(PaymentSheet.ThemeMode.AlwaysLight),
            isSystemDark = true,
        )

        assertConfiguration(
            configuration = configuration,
            expectedDarkMode = TapToPayUxConfiguration.DarkMode.LIGHT,
            expectedPrimary = LIGHT_PRIMARY,
        )
    }

    @Test
    fun `always dark uses dark terminal appearance in system light`() {
        val configuration = createTapToAddUxConfiguration(
            appearance = createAppearance(PaymentSheet.ThemeMode.AlwaysDark),
            isSystemDark = false,
        )

        assertConfiguration(
            configuration = configuration,
            expectedDarkMode = TapToPayUxConfiguration.DarkMode.DARK,
            expectedPrimary = DARK_PRIMARY,
        )
    }

    private fun createAppearance(themeMode: PaymentSheet.ThemeMode): PaymentSheet.Appearance {
        return PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = LIGHT_PRIMARY),
            colorsDark = PaymentSheet.Colors.configureDefaultDark(primary = DARK_PRIMARY),
            themeMode = themeMode,
        )
    }

    private fun assertConfiguration(
        configuration: TapToPayUxConfiguration,
        expectedDarkMode: TapToPayUxConfiguration.DarkMode,
        expectedPrimary: Color,
    ) {
        assertThat(configuration.darkMode).isEqualTo(expectedDarkMode)
        assertThat((configuration.colors.primary as TapToPayUxConfiguration.Color.Value).color)
            .isEqualTo(expectedPrimary.toArgb())
    }

    private companion object {
        val LIGHT_PRIMARY = Color(0xFF123456)
        val DARK_PRIMARY = Color(0xFF654321)
    }
}

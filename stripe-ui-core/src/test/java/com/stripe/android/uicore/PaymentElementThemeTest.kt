package com.stripe.android.uicore

import android.os.Build
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentElementThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    @Config(qualifiers = "notnight")
    fun `always dark overrides system light theme`() = runScenario(
        themeMode = PaymentElementThemeMode.AlwaysDark,
    ) {
        assertThat(isDark).isTrue()
        assertThat(contextIsDark).isTrue()
        assertThat(colors.materialColors.primary).isEqualTo(DARK_PRIMARY)
        assertThat(shapes.cornerRadius).isEqualTo(12f)
        assertThat(typography.fontSizeMultiplier).isEqualTo(1.5f)
        assertThat(primaryButtonStyle.colorsDark.background).isEqualTo(DARK_BUTTON)
        assertThat(formInsets).isEqualTo(FormInsets(start = 1f, top = 2f, end = 3f, bottom = 4f))
        assertThat(sectionSpacing).isEqualTo(10f)
        assertThat(textFieldInsets).isEqualTo(FormInsets(start = 5f, top = 6f, end = 7f, bottom = 8f))
        assertThat(iconStyle).isEqualTo(IconStyle.Outlined)
        assertThat(verticalModeRowPadding).isEqualTo(9f)
    }

    @Test
    @Config(qualifiers = "night")
    fun `always light overrides system dark theme`() = runScenario(
        themeMode = PaymentElementThemeMode.AlwaysLight,
    ) {
        assertThat(isDark).isFalse()
        assertThat(contextIsDark).isFalse()
        assertThat(colors.materialColors.primary).isEqualTo(LIGHT_PRIMARY)
        assertThat(primaryButtonStyle.colorsLight.background).isEqualTo(LIGHT_BUTTON)
    }

    @Test
    @Config(qualifiers = "night")
    fun `automatic follows system dark theme`() = runScenario(
        themeMode = PaymentElementThemeMode.Automatic,
    ) {
        assertThat(isDark).isTrue()
        assertThat(contextIsDark).isTrue()
        assertThat(colors.materialColors.primary).isEqualTo(DARK_PRIMARY)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `automatic follows system light theme`() = runScenario(
        themeMode = PaymentElementThemeMode.Automatic,
    ) {
        assertThat(isDark).isFalse()
        assertThat(contextIsDark).isFalse()
        assertThat(colors.materialColors.primary).isEqualTo(LIGHT_PRIMARY)
    }

    private fun runScenario(
        themeMode: PaymentElementThemeMode,
        block: ThemeSnapshot.() -> Unit,
    ) {
        var snapshot: ThemeSnapshot? = null

        composeRule.setContent {
            PaymentElementTheme(
                values = THEME_VALUES.copy(themeMode = themeMode),
            ) {
                snapshot = ThemeSnapshot(
                    isDark = MaterialTheme.stripeThemeIsDark,
                    contextIsDark = androidx.compose.ui.platform.LocalContext.current.isSystemDarkTheme(),
                    colors = MaterialTheme.stripeColors,
                    shapes = MaterialTheme.stripeShapes,
                    typography = MaterialTheme.stripeTypography,
                    primaryButtonStyle = MaterialTheme.stripePrimaryButtonStyle,
                    formInsets = MaterialTheme.stripeFormInsets,
                    sectionSpacing = LocalSectionSpacing.current,
                    textFieldInsets = LocalTextFieldInsets.current,
                    iconStyle = LocalIconStyle.current,
                    verticalModeRowPadding = MaterialTheme.stripeVerticalModeRowPadding,
                )
            }
        }
        composeRule.waitForIdle()

        requireNotNull(snapshot).apply(block)
    }

    private data class ThemeSnapshot(
        val isDark: Boolean,
        val contextIsDark: Boolean,
        val colors: StripeColors,
        val shapes: StripeShapes,
        val typography: StripeTypography,
        val primaryButtonStyle: PrimaryButtonStyle,
        val formInsets: FormInsets,
        val sectionSpacing: Float?,
        val textFieldInsets: FormInsets,
        val iconStyle: IconStyle,
        val verticalModeRowPadding: Float,
    )

    private companion object {
        val LIGHT_PRIMARY = Color(0xFF123456)
        val DARK_PRIMARY = Color(0xFF654321)
        val LIGHT_BUTTON = Color(0xFFABCDEF)
        val DARK_BUTTON = Color(0xFFFEDCBA)
        val THEME_VALUES = PaymentElementThemeValues(
            colorsLight = StripeThemeDefaults.colorsLight.copy(
                materialColors = StripeThemeDefaults.colorsLight.materialColors.copy(primary = LIGHT_PRIMARY),
            ),
            colorsDark = StripeThemeDefaults.colorsDark.copy(
                materialColors = StripeThemeDefaults.colorsDark.materialColors.copy(primary = DARK_PRIMARY),
            ),
            themeMode = PaymentElementThemeMode.Automatic,
            shapes = StripeThemeDefaults.shapes.copy(cornerRadius = 12f),
            typography = StripeThemeDefaults.typography.copy(fontSizeMultiplier = 1.5f),
            primaryButtonStyle = StripeThemeDefaults.primaryButtonStyle.copy(
                colorsLight = StripeThemeDefaults.primaryButtonStyle.colorsLight.copy(background = LIGHT_BUTTON),
                colorsDark = StripeThemeDefaults.primaryButtonStyle.colorsDark.copy(background = DARK_BUTTON),
            ),
            formInsets = FormInsets(start = 1f, top = 2f, end = 3f, bottom = 4f),
            sectionSpacing = 10f,
            textFieldInsets = FormInsets(start = 5f, top = 6f, end = 7f, bottom = 8f),
            iconStyle = IconStyle.Outlined,
            verticalModeRowPadding = 9f,
        )
    }
}

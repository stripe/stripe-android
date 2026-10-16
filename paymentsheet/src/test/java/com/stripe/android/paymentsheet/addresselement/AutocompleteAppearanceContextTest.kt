@file:OptIn(com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class)

package com.stripe.android.paymentsheet.addresselement

import android.content.res.Configuration
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.isSystemDarkTheme
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.stripeThemeIsDark
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class AutocompleteAppearanceContextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `payment element theme responds to system changes according to its theme mode`(
        @TestParameter themeMode: PaymentSheet.ThemeMode,
    ) = runScenario(themeMode = themeMode) {
        assertTheme(isDark = themeMode == PaymentSheet.ThemeMode.AlwaysDark)

        setSystemDark(true)
        assertTheme(isDark = themeMode != PaymentSheet.ThemeMode.AlwaysLight)

        setSystemDark(false)
        assertTheme(isDark = themeMode == PaymentSheet.ThemeMode.AlwaysDark)
    }

    @Test
    fun `payment element theme uses its appearance after another context applies appearance`() {
        val otherContext = AutocompleteAppearanceContext.PaymentElement(
            appearance = PaymentSheet.Appearance(
                colorsLight = PaymentSheet.Colors.Builder.light().surface(Color.Red).build(),
                colorsDark = PaymentSheet.Colors.Builder.dark().surface(Color.Green).build(),
                shapes = PaymentSheet.Shapes(cornerRadiusDp = 30f, borderStrokeWidthDp = 1f),
            ),
        )

        try {
            otherContext.applyAppearance()

            runScenario(themeMode = PaymentSheet.ThemeMode.Automatic) {
                assertTheme(isDark = false)
                setSystemDark(true)
                assertTheme(isDark = true)
            }
        } finally {
            PaymentSheet.Appearance().parseAppearance()
        }
    }

    private fun runScenario(
        themeMode: PaymentSheet.ThemeMode,
        block: Scenario.() -> Unit,
    ) {
        val appearanceContext = AutocompleteAppearanceContext.PaymentElement(
            appearance = PaymentSheet.Appearance(
                colorsLight = PaymentSheet.Colors.Builder.light().surface(LIGHT_SURFACE).build(),
                colorsDark = PaymentSheet.Colors.Builder.dark().surface(DARK_SURFACE).build(),
                shapes = PaymentSheet.Shapes(cornerRadiusDp = 12f, borderStrokeWidthDp = 1f),
                themeMode = themeMode,
            ),
        )
        var configuration by mutableStateOf(configuration(isDark = false))
        var snapshot: ThemeSnapshot? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                appearanceContext.Theme {
                    snapshot = ThemeSnapshot(
                        isDark = MaterialTheme.stripeThemeIsDark,
                        contextIsDark = LocalContext.current.isSystemDarkTheme(),
                        backgroundColor = appearanceContext.backgroundColor,
                        cornerRadius = MaterialTheme.stripeShapes.cornerRadius,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        Scenario(
            currentTheme = { requireNotNull(snapshot) },
            setSystemDark = { isDark ->
                composeRule.runOnIdle {
                    configuration = configuration(isDark)
                }
                composeRule.waitForIdle()
            },
        ).block()
    }

    private class Scenario(
        val currentTheme: () -> ThemeSnapshot,
        val setSystemDark: (Boolean) -> Unit,
    ) {
        fun assertTheme(isDark: Boolean) {
            val theme = currentTheme()
            assertThat(theme.isDark).isEqualTo(isDark)
            assertThat(theme.contextIsDark).isEqualTo(isDark)
            assertThat(theme.backgroundColor).isEqualTo(if (isDark) DARK_SURFACE else LIGHT_SURFACE)
            assertThat(theme.cornerRadius).isEqualTo(12f)
        }
    }

    private data class ThemeSnapshot(
        val isDark: Boolean,
        val contextIsDark: Boolean,
        val backgroundColor: Color,
        val cornerRadius: Float,
    )

    private fun configuration(isDark: Boolean) = Configuration().apply {
        uiMode = if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    }

    private companion object {
        val LIGHT_SURFACE = Color(0xFFE0ECFF)
        val DARK_SURFACE = Color(0xFF203448)
    }
}

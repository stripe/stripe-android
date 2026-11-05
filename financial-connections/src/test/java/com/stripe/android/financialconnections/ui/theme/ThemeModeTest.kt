package com.stripe.android.financialconnections.ui.theme

import android.content.res.Configuration
import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ThemeMode
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestParameterInjector::class)
internal class ThemeModeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    @Config(qualifiers = "notnight")
    fun `theme mode resolves against a light host`(
        @TestParameter themeMode: ThemeMode,
        @TestParameter linkTheme: Boolean,
    ) = runScenario(themeMode = { themeMode }, linkTheme = linkTheme) {
        assertTheme(isDark = themeMode == ThemeMode.AlwaysDark)
        assertThat(snapshot().hostIsDark).isFalse()
    }

    @Test
    @Config(qualifiers = "night")
    fun `theme mode resolves against a dark host`(
        @TestParameter themeMode: ThemeMode,
        @TestParameter linkTheme: Boolean,
    ) = runScenario(themeMode = { themeMode }, linkTheme = linkTheme) {
        assertTheme(isDark = themeMode != ThemeMode.AlwaysLight)
        assertThat(snapshot().hostIsDark).isTrue()
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `changing theme mode updates colors and resources`() {
        val themeMode = mutableStateOf(ThemeMode.AlwaysDark)
        runScenario(themeMode = { themeMode.value }) {
            assertTheme(isDark = true)

            composeRule.runOnIdle { themeMode.value = ThemeMode.AlwaysLight }
            composeRule.waitForIdle()

            assertTheme(isDark = false)
            assertThat(snapshot().hostIsDark).isFalse()
        }
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `automatic updates with the incoming configuration`() {
        val hostIsDark = mutableStateOf(false)
        runScenario(
            themeMode = { ThemeMode.Automatic },
            configuration = { original ->
                Configuration(original).apply {
                    val nightMode = if (hostIsDark.value) {
                        Configuration.UI_MODE_NIGHT_YES
                    } else {
                        Configuration.UI_MODE_NIGHT_NO
                    }
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
                }
            },
        ) {
            assertTheme(isDark = false)

            composeRule.runOnIdle { hostIsDark.value = true }
            composeRule.waitForIdle()

            assertTheme(isDark = true)
        }
    }

    @Test
    fun `theme override preserves other configuration values`() = runScenario(
        themeMode = { ThemeMode.AlwaysDark },
        configuration = { original ->
            Configuration(original).apply {
                fontScale = 1.4f
                orientation = Configuration.ORIENTATION_LANDSCAPE
                setLocale(Locale.FRENCH)
                uiMode = Configuration.UI_MODE_TYPE_CAR or Configuration.UI_MODE_NIGHT_NO
            }
        },
    ) {
        assertTheme(isDark = true)
        val expected = Configuration(snapshot().incomingConfiguration).apply {
            uiMode = Configuration.UI_MODE_TYPE_CAR or Configuration.UI_MODE_NIGHT_YES
        }
        assertThat(snapshot().configuration).isEqualTo(expected)
        assertThat(snapshot().resourceConfiguration).isEqualTo(expected)
    }

    private fun runScenario(
        themeMode: () -> ThemeMode,
        linkTheme: Boolean = false,
        configuration: (Configuration) -> Configuration = { it },
        block: Scenario.() -> Unit,
    ) {
        var snapshot: ThemeSnapshot? = null
        composeRule.setContent {
            val hostContext = LocalContext.current
            val incomingConfiguration = configuration(LocalConfiguration.current)
            CompositionLocalProvider(LocalConfiguration provides incomingConfiguration) {
                val theme = if (linkTheme) Theme.LinkLight(themeMode()) else Theme.DefaultLight(themeMode())
                FinancialConnectionsTheme(theme) {
                    val resources = LocalResources.current
                    snapshot = ThemeSnapshot(
                        isDark = isSystemInDarkTheme(),
                        hostIsDark = hostContext.resources.configuration.isDark(),
                        contextIsDark = LocalContext.current.resources.configuration.isDark(),
                        configuration = Configuration(LocalConfiguration.current),
                        resourceConfiguration = Configuration(resources.configuration),
                        incomingConfiguration = Configuration(incomingConfiguration),
                        background = FinancialConnectionsTheme.colors.background,
                        logoPath = resources.logoPath(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        Scenario { requireNotNull(snapshot) }.block()
    }

    private class Scenario(val snapshot: () -> ThemeSnapshot) {
        fun assertTheme(isDark: Boolean) {
            val actual = snapshot()
            assertThat(actual.isDark).isEqualTo(isDark)
            assertThat(actual.contextIsDark).isEqualTo(isDark)
            assertThat(actual.resourceConfiguration.isDark()).isEqualTo(isDark)
            assertThat(actual.background).isEqualTo(if (isDark) Neutral0Dark else Neutral0)
            if (isDark) {
                assertThat(actual.logoPath).contains("drawable-night")
            } else {
                assertThat(actual.logoPath).doesNotContain("drawable-night")
            }
        }
    }

    private data class ThemeSnapshot(
        val isDark: Boolean,
        val hostIsDark: Boolean,
        val contextIsDark: Boolean,
        val configuration: Configuration,
        val resourceConfiguration: Configuration,
        val incomingConfiguration: Configuration,
        val background: Color,
        val logoPath: String,
    )
}

private fun Configuration.isDark(): Boolean =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

private fun Resources.logoPath(): String = TypedValue().let { value ->
    getValue(R.drawable.stripe_link_logo_monochrome, value, true)
    value.string.toString()
}

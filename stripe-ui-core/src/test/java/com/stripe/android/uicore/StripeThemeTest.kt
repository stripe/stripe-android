package com.stripe.android.uicore

import android.os.Build
import androidx.compose.material.MaterialTheme
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
internal class StripeThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `explicit dark theme is provided to descendants`() = runScenario(isDark = true) {
        assertThat(isDark).isTrue()
    }

    @Test
    fun `explicit light theme is provided to descendants`() = runScenario(isDark = false) {
        assertThat(isDark).isFalse()
    }

    @Test
    @Config(qualifiers = "night")
    fun `legacy theme delegates with system dark mode`() {
        var isDark: Boolean? = null

        composeRule.setContent {
            StripeTheme {
                isDark = MaterialTheme.stripeThemeIsDark
            }
        }
        composeRule.waitForIdle()

        assertThat(isDark).isTrue()
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `legacy theme delegates with system light mode`() {
        var isDark: Boolean? = null

        composeRule.setContent {
            StripeTheme {
                isDark = MaterialTheme.stripeThemeIsDark
            }
        }
        composeRule.waitForIdle()

        assertThat(isDark).isFalse()
    }

    private fun runScenario(
        isDark: Boolean,
        block: Scenario.() -> Unit,
    ) {
        var providedIsDark: Boolean? = null

        composeRule.setContent {
            StripeTheme(
                isDark = isDark,
                colors = StripeThemeDefaults.colors(isDark),
                shapes = StripeThemeDefaults.shapes,
                typography = StripeThemeDefaults.typography,
                primaryButtonStyle = StripeThemeDefaults.primaryButtonStyle,
                formInsets = StripeThemeDefaults.formInsets,
                sectionSpacing = StripeThemeDefaults.sectionSpacing,
                sectionStyle = StripeThemeDefaults.sectionStyle,
                textFieldInsets = StripeThemeDefaults.textFieldInsets,
                iconStyle = StripeThemeDefaults.iconStyle,
                verticalModeRowPadding = StripeThemeDefaults.verticalModeRowPadding,
            ) {
                providedIsDark = MaterialTheme.stripeThemeIsDark
            }
        }
        composeRule.waitForIdle()

        Scenario(isDark = requireNotNull(providedIsDark)).apply(block)
    }

    private data class Scenario(
        val isDark: Boolean,
    )
}

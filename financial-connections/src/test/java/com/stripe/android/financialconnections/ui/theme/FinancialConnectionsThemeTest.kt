package com.stripe.android.financialconnections.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class FinancialConnectionsThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    @Config(qualifiers = "night")
    fun `default theme uses dark background in dark mode`() {
        assertBackgroundColor(
            theme = Theme.DefaultLight,
            expected = Color(0xFF171717),
        )
    }

    @Test
    @Config(qualifiers = "night")
    fun `link theme uses dark background in dark mode`() {
        assertBackgroundColor(
            theme = Theme.LinkLight,
            expected = Color(0xFF171717),
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `default theme uses white background in light mode`() {
        assertBackgroundColor(
            theme = Theme.DefaultLight,
            expected = Color.White,
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `link theme uses white background in light mode`() {
        assertBackgroundColor(
            theme = Theme.LinkLight,
            expected = Color.White,
        )
    }

    private fun assertBackgroundColor(theme: Theme, expected: Color) {
        var background: Color? = null

        composeRule.setContent {
            FinancialConnectionsTheme(theme = theme) {
                background = FinancialConnectionsTheme.colors.background
            }
        }
        composeRule.waitForIdle()

        assertThat(background).isEqualTo(expected)
    }
}

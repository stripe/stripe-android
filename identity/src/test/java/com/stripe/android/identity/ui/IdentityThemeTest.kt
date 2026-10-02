package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityActivity
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@OptIn(ExperimentalCoroutinesApi::class)
internal class IdentityThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `brand color is applied with fallback theme`() = runScenario(
        hostTheme = AppCompatR.style.Theme_AppCompat_Light_NoActionBar,
    ) {
        assertThat(colors.primary).isEqualTo(Color(BRAND_COLOR))
    }

    @Test
    fun `brand color is applied with Material host theme`() = runScenario(
        hostTheme = MaterialR.style.Theme_MaterialComponents_Light_NoActionBar,
    ) {
        assertThat(colors.primary).isEqualTo(Color(BRAND_COLOR))
    }

    private fun runScenario(
        hostTheme: Int,
        block: Scenario.() -> Unit,
    ) {
        Robolectric.buildActivity(IdentityActivity::class.java).use { controller ->
            val activity = controller.get()
            activity.setTheme(hostTheme)
            activity.ensureCompatibleTheme()
            var providedColors: Colors? = null

            composeRule.setContent {
                CompositionLocalProvider(LocalContext provides activity) {
                    IdentityTheme(brandColor = BRAND_COLOR) {
                        providedColors = MaterialTheme.colors
                    }
                }
            }
            composeRule.waitForIdle()

            Scenario(colors = requireNotNull(providedColors)).apply(block)
        }
    }

    private data class Scenario(val colors: Colors)

    private companion object {
        const val BRAND_COLOR = 0xFF12AB34.toInt()
    }
}

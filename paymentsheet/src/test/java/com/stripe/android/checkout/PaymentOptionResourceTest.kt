package com.stripe.android.checkout

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionResourceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `automatic appearance reloads when system theme changes`() = runScenario(
        themeMode = PaymentSheet.ThemeMode.Automatic,
    ) {
        assertThat(loadCalls.awaitItem()).isFalse()

        setSystemDarkTheme(true)

        assertThat(loadCalls.awaitItem()).isTrue()
    }

    @Test
    fun `always light appearance does not reload when system theme changes`() = runScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysLight,
    ) {
        assertThat(loadCalls.awaitItem()).isFalse()

        setSystemDarkTheme(true)

        loadCalls.expectNoEvents()
    }

    @Test
    fun `always dark appearance does not reload when system theme changes`() = runScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysDark,
    ) {
        assertThat(loadCalls.awaitItem()).isTrue()

        setSystemDarkTheme(true)

        loadCalls.expectNoEvents()
    }

    private fun runScenario(
        themeMode: PaymentSheet.ThemeMode,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val isSystemDarkTheme = mutableStateOf(false)
        val loadCalls = Turbine<Boolean>()
        val resource = PaymentOptionResource(
            appearance = PaymentSheet.Appearance.Builder()
                .colorsLight(
                    PaymentSheet.Colors.Builder.light()
                        .component(Color.White)
                        .build()
                )
                .colorsDark(
                    PaymentSheet.Colors.Builder.dark()
                        .component(Color.Black)
                        .build()
                )
                .themeMode(themeMode)
                .build(),
            loader = { useDarkThemeIcon ->
                loadCalls.add(useDarkThemeIcon)
                ColorDrawable()
            },
        )

        composeRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                val nightMode = if (isSystemDarkTheme.value) {
                    Configuration.UI_MODE_NIGHT_YES
                } else {
                    Configuration.UI_MODE_NIGHT_NO
                }
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                rememberPaymentOptionResource(resource)
            }
        }
        composeRule.waitForIdle()

        Scenario(
            loadCalls = loadCalls,
            setSystemDarkTheme = { isDarkTheme ->
                composeRule.runOnIdle {
                    isSystemDarkTheme.value = isDarkTheme
                }
                composeRule.waitForIdle()
            },
        ).block()
        loadCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val loadCalls: ReceiveTurbine<Boolean>,
        val setSystemDarkTheme: (Boolean) -> Unit,
    )
}

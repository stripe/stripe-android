package com.stripe.android.common.nfcscan.ui

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class ErrorBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    val stateRestorer = StateRestorationTester(composeRule)

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun `error message shows error banner`() {
        stateRestorer.setContent {
            ErrorBanner(
                error = ErrorBannerParams(
                    message = ERROR_MESSAGE,
                    onShown = {},
                ),
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ERROR_BANNER_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(ERROR_MESSAGE_TEXT, substring = true).assertIsDisplayed()
    }

    @Test
    fun `error invokes onShown after delay`() {
        var errorShownCount by mutableIntStateOf(0)

        stateRestorer.setContent {
            ErrorBanner(
                error = ErrorBannerParams(
                    message = ERROR_MESSAGE,
                    onShown = { errorShownCount++ },
                ),
            )
        }

        composeRule.waitForIdle()
        assertThat(errorShownCount).isEqualTo(0)

        composeRule.advanceErrorDelayBy(ERROR_SHOWN_DELAY_MS + FRAME_BUFFER_MS)
        assertThat(errorShownCount).isEqualTo(1)
    }

    @Test
    fun `config change during error delay resumes remaining delay`() {
        var errorShownCount by mutableIntStateOf(0)

        stateRestorer.setErrorBannerContent(
            error = ERROR_MESSAGE,
            onShown = { errorShownCount++ },
        )

        composeRule.waitForIdle()

        val elapsedDelayMs = ERROR_SHOWN_DELAY_MS / 2
        composeRule.advanceErrorDelayBy(elapsedDelayMs)
        assertThat(errorShownCount).isEqualTo(0)

        stateRestorer.emulateSavedInstanceStateRestore()
        assertThat(errorShownCount).isEqualTo(0)

        val remainingDelayMs = ERROR_SHOWN_DELAY_MS - elapsedDelayMs
        composeRule.advanceErrorDelayBy(remainingDelayMs / 2)
        assertThat(errorShownCount).isEqualTo(0)

        composeRule.advanceErrorDelayBy(remainingDelayMs / 2 + FRAME_BUFFER_MS)
        assertThat(errorShownCount).isEqualTo(1)
    }

    @Test
    fun `config change after error shown does not invoke onShown again`() {
        var errorShownCount by mutableIntStateOf(0)

        stateRestorer.setErrorBannerContent(
            error = ERROR_MESSAGE,
            onShown = { errorShownCount++ },
        )

        composeRule.waitForIdle()
        composeRule.advanceErrorDelayBy(ERROR_SHOWN_DELAY_MS + FRAME_BUFFER_MS)
        assertThat(errorShownCount).isEqualTo(1)

        stateRestorer.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(errorShownCount).isEqualTo(1)
        composeRule.onNodeWithTag(ERROR_BANNER_TEST_TAG).assertIsDisplayed()
    }

    private fun ComposeContentTestRule.advanceErrorDelayBy(durationMs: Long) {
        ShadowSystemClock.advanceBy(durationMs, TimeUnit.MILLISECONDS)
        mainClock.advanceTimeBy(durationMs)
        waitForIdle()
    }

    private fun StateRestorationTester.setErrorBannerContent(
        error: ResolvableString,
        onShown: () -> Unit = {},
    ) {
        setContent {
            ErrorBanner(
                error = ErrorBannerParams(
                    message = error,
                    onShown = onShown,
                ),
            )
        }
    }

    private companion object {
        const val FRAME_BUFFER_MS = 32L
        const val ERROR_SHOWN_DELAY_MS = 3_000L
        const val ERROR_MESSAGE_TEXT = "Card expired. Try another card."
        val ERROR_MESSAGE = ERROR_MESSAGE_TEXT.resolvableString
    }
}

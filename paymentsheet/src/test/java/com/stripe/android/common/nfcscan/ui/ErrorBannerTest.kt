package com.stripe.android.common.nfcscan.ui

import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
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

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun `error message shows error banner`() {
        composeRule.setContent {
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

        composeRule.setContent {
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
        val visible = mutableStateOf(true)

        composeRule.setErrorBannerContent(
            visible = visible,
            error = ERROR_MESSAGE,
            onShown = { errorShownCount++ },
        )

        composeRule.waitForIdle()

        val elapsedDelayMs = ERROR_SHOWN_DELAY_MS / 2
        composeRule.advanceErrorDelayBy(elapsedDelayMs)
        assertThat(errorShownCount).isEqualTo(0)

        composeRule.simulateConfigChange(visible)
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
        val visible = mutableStateOf(true)

        composeRule.setErrorBannerContent(
            visible = visible,
            error = ERROR_MESSAGE,
            onShown = { errorShownCount++ },
        )

        composeRule.waitForIdle()
        composeRule.advanceErrorDelayBy(ERROR_SHOWN_DELAY_MS + FRAME_BUFFER_MS)
        assertThat(errorShownCount).isEqualTo(1)

        composeRule.simulateConfigChange(visible)
        composeRule.waitForIdle()

        assertThat(errorShownCount).isEqualTo(1)
        composeRule.onNodeWithTag(ERROR_BANNER_TEST_TAG).assertIsDisplayed()
    }

    private fun ComposeContentTestRule.advanceErrorDelayBy(durationMs: Long) {
        ShadowSystemClock.advanceBy(durationMs, TimeUnit.MILLISECONDS)
        mainClock.advanceTimeBy(durationMs)
        waitForIdle()
    }

    private fun ComposeContentTestRule.setErrorBannerContent(
        visible: MutableState<Boolean>,
        error: ResolvableString,
        onShown: () -> Unit = {},
    ) {
        setContent {
            val saveableStateHolder = rememberSaveableStateHolder()

            if (visible.value) {
                saveableStateHolder.SaveableStateProvider(ERROR_BANNER_SAVABLE_KEY) {
                    ErrorBanner(
                        error = ErrorBannerParams(
                            message = error,
                            onShown = onShown,
                        ),
                    )
                }
            }
        }
    }

    private fun ComposeContentTestRule.simulateConfigChange(visible: MutableState<Boolean>) {
        runOnUiThread { visible.value = false }
        waitForIdle()
        runOnUiThread { visible.value = true }
        waitForIdle()
    }

    private companion object {
        const val ERROR_BANNER_SAVABLE_KEY = "nfc_error_banner"
        const val FRAME_BUFFER_MS = 32L
        const val ERROR_SHOWN_DELAY_MS = 3_000L
        const val ERROR_MESSAGE_TEXT = "Card expired. Try another card."
        val ERROR_MESSAGE = ERROR_MESSAGE_TEXT.resolvableString
    }
}

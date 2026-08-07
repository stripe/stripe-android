package com.stripe.android.common.nfcscan.ui

import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
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
internal class NfcCoilAnimatedInteriorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun `idle status shows NFC icon`() {
        composeRule.setContent {
            NfcCoilAnimatedInterior(
                status = NfcScanningStatus.Idle(error = null),
                onSuccessShown = {},
                modifier = Modifier.size(CoilSize),
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NFC_COIL_CONTACTLESS_ICON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHECKMARK_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `scanning status shows NFC icon`() {
        composeRule.setContent {
            NfcCoilAnimatedInterior(
                status = NfcScanningStatus.Scanning,
                onSuccessShown = {},
                modifier = Modifier.size(CoilSize),
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NFC_COIL_CONTACTLESS_ICON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHECKMARK_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `scanned status animates checkmark draw`() {
        composeRule.setContent {
            NfcCoilAnimatedInterior(
                status = NfcScanningStatus.Scanned,
                onSuccessShown = {},
                modifier = Modifier.size(CoilSize),
            )
        }

        composeRule.waitForIdle()
        assertThat(composeRule.checkmarkProgress().progress).isEqualTo(0f)

        composeRule.mainClock.advanceTimeBy((CHECKMARK_DRAW_DURATION_MS / 2).toLong())
        composeRule.waitForIdle()

        val progressMidAnimation = composeRule.checkmarkProgress().progress
        assertThat(progressMidAnimation).isGreaterThan(0.1f)
        assertThat(progressMidAnimation).isLessThan(0.99f)

        composeRule.mainClock.advanceTimeBy(
            (CHECKMARK_DRAW_DURATION_MS / 2).toLong() + FRAME_BUFFER_MS,
        )
        composeRule.waitForIdle()

        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())
    }

    @Test
    fun `scanned status invokes onSuccessShown after animation and delay`() {
        var successShownCount by mutableIntStateOf(0)

        composeRule.mainClock.autoAdvance = true

        composeRule.setContent {
            NfcCoilAnimatedInterior(
                status = NfcScanningStatus.Scanned,
                onSuccessShown = { successShownCount++ },
                modifier = Modifier.size(CoilSize),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            successShownCount == 1
        }

        assertThat(successShownCount).isEqualTo(1)
    }

    @Test
    fun `config change during checkmark animation resumes from saved progress`() {
        val visible = mutableStateOf(true)

        composeRule.setNfcCoilContent(
            visible = visible,
            status = NfcScanningStatus.Scanned,
        )

        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy((CHECKMARK_DRAW_DURATION_MS / 2).toLong())
        composeRule.waitForIdle()

        val progressBeforeConfigChange = composeRule.checkmarkProgress()
        assertThat(progressBeforeConfigChange.progress).isGreaterThan(0.1f)
        assertThat(progressBeforeConfigChange.progress).isLessThan(0.99f)

        composeRule.simulateConfigChange(visible)

        val progressAfterConfigChange = composeRule.checkmarkProgress()
        assertThat(progressAfterConfigChange.progress)
            .isWithin(PROGRESS_TOLERANCE)
            .of(progressBeforeConfigChange.progress)
        assertThat(progressAfterConfigChange.progress).isGreaterThan(0.1f)
    }

    @Test
    fun `config change after animation complete does not restart success animation`() {
        val visible = mutableStateOf(true)

        composeRule.setNfcCoilContent(
            visible = visible,
            status = NfcScanningStatus.Scanned,
        )

        composeRule.waitForIdle()
        composeRule.advanceThroughSuccessAnimation()
        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())

        composeRule.simulateConfigChange(visible)

        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())
    }

    @Test
    fun `config change after success shown does not invoke onSuccessShown again`() {
        var successShownCount by mutableIntStateOf(0)
        val visible = mutableStateOf(true)

        composeRule.mainClock.autoAdvance = true
        composeRule.setNfcCoilContent(
            visible = visible,
            status = NfcScanningStatus.Scanned,
            onSuccessShown = { successShownCount++ },
        )

        composeRule.waitUntil(timeoutMillis = 5_000) {
            successShownCount == 1
        }

        composeRule.simulateConfigChange(visible)
        composeRule.waitForIdle()

        assertThat(successShownCount).isEqualTo(1)
        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())
    }

    @Test
    fun `config change during success delay resumes remaining delay`() {
        var successShownCount by mutableIntStateOf(0)
        val visible = mutableStateOf(true)

        composeRule.setNfcCoilContent(
            visible = visible,
            status = NfcScanningStatus.Scanned,
            onSuccessShown = { successShownCount++ },
        )

        composeRule.waitForIdle()
        composeRule.advanceThroughSuccessAnimation()

        val elapsedDelayMs = SUCCESS_SHOWN_DELAY_MS / 2
        composeRule.advanceSuccessDelayBy(elapsedDelayMs)
        assertThat(successShownCount).isEqualTo(0)

        composeRule.simulateConfigChange(visible)
        assertThat(successShownCount).isEqualTo(0)

        val remainingDelayMs = SUCCESS_SHOWN_DELAY_MS - elapsedDelayMs
        composeRule.advanceSuccessDelayBy(remainingDelayMs / 2)
        assertThat(successShownCount).isEqualTo(0)

        composeRule.advanceSuccessDelayBy(remainingDelayMs / 2 + FRAME_BUFFER_MS)
        assertThat(successShownCount).isEqualTo(1)
    }

    private fun ComposeContentTestRule.advanceThroughSuccessAnimation() {
        val durationMs = CHECKMARK_DRAW_DURATION_MS.toLong() + FRAME_BUFFER_MS
        ShadowSystemClock.advanceBy(durationMs, TimeUnit.MILLISECONDS)
        mainClock.advanceTimeBy(durationMs)
        waitForIdle()
    }

    private fun ComposeContentTestRule.advanceSuccessDelayBy(durationMs: Long) {
        ShadowSystemClock.advanceBy(durationMs, TimeUnit.MILLISECONDS)
        mainClock.advanceTimeBy(durationMs)
        waitForIdle()
    }

    private fun ComposeContentTestRule.setNfcCoilContent(
        visible: MutableState<Boolean>,
        status: NfcScanningStatus,
        onSuccessShown: () -> Unit = {},
    ) {
        setContent {
            val saveableStateHolder = rememberSaveableStateHolder()

            if (visible.value) {
                saveableStateHolder.SaveableStateProvider(NFC_COIL_SAVABLE_KEY) {
                    NfcCoilAnimatedInterior(
                        status = status,
                        onSuccessShown = onSuccessShown,
                        modifier = Modifier.size(CoilSize),
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

    private fun assertCheckmarkProgressComplete(progress: CheckmarkProgress) {
        assertThat(progress.progress).isWithin(PROGRESS_TOLERANCE).of(1f)
    }

    private fun ComposeContentTestRule.checkmarkProgress(): CheckmarkProgress {
        return onNodeWithTag(CHECKMARK_TEST_TAG)
            .fetchSemanticsNode()
            .config[CheckmarkProgressKey]
    }

    private companion object {
        const val NFC_COIL_SAVABLE_KEY = "nfc_coil_animated_interior"
        const val FRAME_BUFFER_MS = 32L
        val CoilSize = 200.dp
        const val PROGRESS_TOLERANCE = 0.02f
        const val SUCCESS_SHOWN_DELAY_MS = 900L
    }
}

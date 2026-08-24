package com.stripe.android.common.nfcscan.ui

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    val stateRestorer = StateRestorationTester(composeRule)

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun `idle status shows NFC icon`() {
        composeRule.setContent {
            NfcCoilAnimatedInterior(
                status = NfcScanningStatus.Idle(),
                onSuccessShown = {},
                modifier = Modifier.size(CoilSize),
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NFC_COIL_CONTACTLESS_ICON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHECKMARK_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(ERROR_BANNER_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `scanning status shows spinner`() {
        composeRule.setContent {
            NfcCoilAnimatedInterior(
                status = NfcScanningStatus.Scanning,
                onSuccessShown = {},
                modifier = Modifier.size(CoilSize),
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SPINNER_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(NFC_COIL_CONTACTLESS_ICON_TEST_TAG).assertDoesNotExist()
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
        assertThat(composeRule.checkmarkProgress()).isEqualTo(0f)

        composeRule.advanceTimeBy(CHECKMARK_START_DELAY_MS + (CHECKMARK_DRAW_DURATION_MS / 2).toLong())

        val progressMidAnimation = composeRule.checkmarkProgress()
        assertThat(progressMidAnimation).isGreaterThan(0.1f)
        assertThat(progressMidAnimation).isLessThan(0.99f)

        composeRule.advanceTimeBy(
            (CHECKMARK_DRAW_DURATION_MS / 2).toLong() + FRAME_BUFFER_MS,
        )

        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())
    }

    @Test
    fun `scanned status invokes onSuccessShown after animation and delay`() {
        var successShownCount by mutableIntStateOf(0)

        composeRule.setContent {
            NfcCoilAnimatedInterior(
                status = NfcScanningStatus.Scanned,
                onSuccessShown = { successShownCount++ },
                modifier = Modifier.size(CoilSize),
            )
        }

        composeRule.waitForIdle()
        composeRule.advanceThroughSuccessAnimation()
        composeRule.advanceSuccessDelayBy(SUCCESS_SHOWN_DELAY_MS + FRAME_BUFFER_MS)

        assertThat(successShownCount).isEqualTo(1)
    }

    @Test
    fun `config change during checkmark animation resumes from saved progress`() {
        stateRestorer.setNfcCoilContent(
            status = NfcScanningStatus.Scanned,
        )

        composeRule.waitForIdle()
        composeRule.advanceTimeBy(CHECKMARK_START_DELAY_MS + (CHECKMARK_DRAW_DURATION_MS / 2).toLong())

        val progressBeforeConfigChange = composeRule.checkmarkProgress()
        assertThat(progressBeforeConfigChange).isGreaterThan(0.1f)
        assertThat(progressBeforeConfigChange).isLessThan(0.99f)

        stateRestorer.emulateSavedInstanceStateRestore()

        val progressAfterConfigChange = composeRule.checkmarkProgress()
        assertThat(progressAfterConfigChange)
            .isWithin(PROGRESS_TOLERANCE)
            .of(progressBeforeConfigChange)
        assertThat(progressAfterConfigChange).isGreaterThan(0.1f)
    }

    @Test
    fun `config change after animation complete does not restart success animation`() {
        stateRestorer.setNfcCoilContent(
            status = NfcScanningStatus.Scanned,
        )

        composeRule.waitForIdle()
        composeRule.advanceThroughSuccessAnimation()
        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())

        stateRestorer.emulateSavedInstanceStateRestore()

        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())
    }

    @Test
    fun `config change after success shown does not invoke onSuccessShown again`() {
        var successShownCount by mutableIntStateOf(0)

        stateRestorer.setNfcCoilContent(
            status = NfcScanningStatus.Scanned,
            onSuccessShown = { successShownCount++ },
        )

        composeRule.waitForIdle()
        composeRule.advanceThroughSuccessAnimation()
        composeRule.advanceSuccessDelayBy(SUCCESS_SHOWN_DELAY_MS + FRAME_BUFFER_MS)
        assertThat(successShownCount).isEqualTo(1)

        stateRestorer.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(successShownCount).isEqualTo(1)
        assertCheckmarkProgressComplete(composeRule.checkmarkProgress())
    }

    @Test
    fun `config change during success delay resumes remaining delay`() {
        var successShownCount by mutableIntStateOf(0)

        stateRestorer.setNfcCoilContent(
            status = NfcScanningStatus.Scanned,
            onSuccessShown = { successShownCount++ },
        )

        composeRule.waitForIdle()
        composeRule.advanceThroughSuccessAnimation()

        val elapsedDelayMs = SUCCESS_SHOWN_DELAY_MS / 2
        composeRule.advanceSuccessDelayBy(elapsedDelayMs)
        assertThat(successShownCount).isEqualTo(0)

        stateRestorer.emulateSavedInstanceStateRestore()
        assertThat(successShownCount).isEqualTo(0)

        val remainingDelayMs = SUCCESS_SHOWN_DELAY_MS - elapsedDelayMs
        composeRule.advanceSuccessDelayBy(remainingDelayMs / 2)
        assertThat(successShownCount).isEqualTo(0)

        composeRule.advanceSuccessDelayBy(remainingDelayMs / 2 + FRAME_BUFFER_MS)
        assertThat(successShownCount).isEqualTo(1)
    }

    private fun ComposeContentTestRule.advanceThroughSuccessAnimation() {
        val durationMs = CHECKMARK_START_DELAY_MS + CHECKMARK_DRAW_DURATION_MS + FRAME_BUFFER_MS
        advanceTimeBy(durationMs)
    }

    private fun ComposeContentTestRule.advanceSuccessDelayBy(durationMs: Long) {
        advanceTimeBy(durationMs)
    }

    private fun ComposeContentTestRule.advanceTimeBy(durationMs: Long) {
        ShadowSystemClock.advanceBy(durationMs, TimeUnit.MILLISECONDS)
        mainClock.advanceTimeBy(durationMs)
        waitForIdle()
    }

    private fun StateRestorationTester.setNfcCoilContent(
        status: NfcScanningStatus,
        onSuccessShown: () -> Unit = {},
    ) {
        setContent {
            NfcCoilAnimatedInterior(
                status = status,
                onSuccessShown = onSuccessShown,
                modifier = Modifier.size(CoilSize),
            )
        }
    }

    private fun assertCheckmarkProgressComplete(progress: Float) {
        assertThat(progress).isWithin(PROGRESS_TOLERANCE).of(1f)
    }

    private fun ComposeContentTestRule.checkmarkProgress(): Float {
        return onNodeWithTag(CHECKMARK_TEST_TAG)
            .fetchSemanticsNode()
            .config[CheckmarkProgressKey]
    }

    private companion object {
        const val FRAME_BUFFER_MS = 32L
        val CoilSize = 200.dp
        const val PROGRESS_TOLERANCE = 0.02f
        const val CHECKMARK_START_DELAY_MS = 150L
        const val SUCCESS_SHOWN_DELAY_MS = 900L
    }
}

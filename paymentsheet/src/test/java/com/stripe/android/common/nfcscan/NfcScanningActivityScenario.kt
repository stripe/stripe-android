package com.stripe.android.common.nfcscan

import android.os.Looper
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNfcAdapter

internal class NfcScanningActivityScenario(
    private val activityScenario: ActivityScenario<NfcScanningActivity>,
    val composeRule: ComposeTestRule,
    val isoDep: FakeIsoDep,
    val nfcAdapter: ShadowNfcAdapter?,
) {
    fun waitForIdle() {
        shadowOf(Looper.getMainLooper()).idle()
        Espresso.onIdle()
        composeRule.waitForIdle()
    }

    fun dispatchCardRead(
        apduResponses: List<ByteArray>,
    ) {
        isoDep.setTransceiveResponses(apduResponses)
        nfcAdapter?.dispatchTagDiscovered(isoDep.tag)
        waitForIdle()
    }

    fun waitForUi() {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            waitForIdle()
            composeRule.onAllNodesWithContentDescription("Cancel")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    fun waitForCompleteUi() {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            waitForIdle()
            composeRule.onAllNodesWithContentDescription("Cancel")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    fun assertErrorIsDisplayed(errorText: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            waitForIdle()
            composeRule.onAllNodesWithText(errorText, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithText(errorText, substring = true).assertExists()
    }

    fun getResult(): NfcScanningContract.Result {
        return NfcScanningContract.parseResult(
            resultCode = activityScenario.result.resultCode,
            intent = activityScenario.result.resultData,
        )
    }

    fun moveToState(state: Lifecycle.State) {
        activityScenario.moveToState(state)
    }

    fun isActivityDestroyed(): Boolean {
        return activityScenario.state == Lifecycle.State.DESTROYED
    }

    private companion object {
        const val UI_TIMEOUT_MS = 5_000L
    }
}

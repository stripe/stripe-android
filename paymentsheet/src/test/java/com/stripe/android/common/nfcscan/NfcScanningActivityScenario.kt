package com.stripe.android.common.nfcscan

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import com.stripe.android.common.nfcscan.ui.ERROR_CROSS_TEST_TAG
import com.stripe.android.common.nfcscan.ui.NFC_COIL_CONTACTLESS_ICON_TEST_TAG
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNfcAdapter

internal class NfcScanningActivityScenario(
    private val activityScenario: ActivityScenario<NfcScanningActivity>,
    val composeRule: ComposeTestRule,
    val isoDep: FakeIsoDep,
    val nfcAdapter: ShadowNfcAdapter?,
    val paymentMethodMetadata: PaymentMethodMetadata,
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
        composeRule.mainClock.advanceTimeUntil(timeoutMillis = ERROR_TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(ERROR_CROSS_TEST_TAG)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeRule.onNodeWithTag(ERROR_CROSS_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(errorText, substring = true).assertIsDisplayed()
    }

    fun assertErrorDisappears() {
        composeRule.mainClock.advanceTimeUntil(timeoutMillis = ERROR_TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(ERROR_CROSS_TEST_TAG)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }

        composeRule.onNodeWithTag(NFC_COIL_CONTACTLESS_ICON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(ERROR_CROSS_TEST_TAG).assertIsNotDisplayed()
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
        const val ERROR_TIMEOUT_MS = 10_000L
    }
}

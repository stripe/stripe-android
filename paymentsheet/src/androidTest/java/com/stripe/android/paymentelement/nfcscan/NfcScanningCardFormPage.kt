package com.stripe.android.paymentelement.nfcscan

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stripe.android.common.taptoadd.TAP_TO_BUTTON_UI_TEST_TAG
import com.stripe.android.testing.replaceText
import com.stripe.android.testing.waitForExactlyOneNode
import com.stripe.android.testing.waitForNode

internal class NfcScanningCardFormPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickOnNfcScan() {
        composeTestRule.waitForExactlyOneNode(
            matcher = hasTestTag(TAP_TO_BUTTON_UI_TEST_TAG),
            timeoutMillis = UI_TIMEOUT_MS,
            atLeastOneRootRequired = false,
        )

        composeTestRule.onNode(hasTestTag(TAP_TO_BUTTON_UI_TEST_TAG))
            .assertIsEnabled()
            .performClick()
    }

    fun fillRemainingCardDetails(
        cvc: String = "123",
        zipCode: String = "12345",
    ) {
        composeTestRule.replaceText(
            matcher = hasText("CVC"),
            text = cvc,
        )
        composeTestRule.replaceText(
            matcher = hasText("ZIP Code"),
            text = zipCode,
        )
        composeTestRule.waitForIdle()
    }

    fun assertScannedCardShown(
        lastFourDigits: String,
    ) {
        composeTestRule.waitForNode(
            matcher = hasText("•••• $lastFourDigits"),
            timeoutMillis = UI_TIMEOUT_MS,
            atLeastOneRootRequired = false,
        )

        composeTestRule.onNodeWithText("•••• $lastFourDigits").assertExists()
        composeTestRule.onNodeWithContentDescription(CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION).assertExists()
    }

    fun assertCvcIsFocused() {
        composeTestRule.waitUntil(UI_TIMEOUT_MS) {
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodes(hasText("CVC").and(isFocused()))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val UI_TIMEOUT_MS = 5_000L
        const val CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION = "Clear scanned card"
    }
}

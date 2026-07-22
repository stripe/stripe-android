package com.stripe.android.paymentelement.nfcscan

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import com.stripe.android.common.taptoadd.TAP_TO_BUTTON_UI_TEST_TAG

internal class NfcScanningCardFormPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickOnNfcScan() {
        composeTestRule.waitUntil(UI_TIMEOUT_MS) {
            composeTestRule.onAllNodes(hasTestTag(TAP_TO_BUTTON_UI_TEST_TAG))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }

        composeTestRule.onNode(hasTestTag(TAP_TO_BUTTON_UI_TEST_TAG))
            .assertIsEnabled()
            .performClick()
    }

    fun fillRemainingCardDetails(
        cvc: String = "123",
        zipCode: String = "12345",
    ) {
        composeTestRule.onNode(hasText("CVC")).performTextReplacement(cvc)
        composeTestRule.onNode(hasText("ZIP Code")).performTextReplacement(zipCode)
        composeTestRule.waitForIdle()
    }

    fun assertScannedCardShown(
        lastFourDigits: String,
    ) {
        composeTestRule.waitUntil(UI_TIMEOUT_MS) {
            composeTestRule.onAllNodes(hasText("•••• $lastFourDigits"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("•••• $lastFourDigits").assertExists()
        composeTestRule.onNodeWithContentDescription(CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION).assertExists()
    }

    private companion object {
        const val UI_TIMEOUT_MS = 5_000L
        const val CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION = "Clear scanned card"
    }
}

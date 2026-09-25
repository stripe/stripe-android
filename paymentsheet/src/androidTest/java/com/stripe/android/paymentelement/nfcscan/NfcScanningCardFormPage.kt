package com.stripe.android.paymentelement.nfcscan

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.stripe.android.common.taptoadd.TAP_TO_BUTTON_UI_TEST_TAG
import com.stripe.android.paymentsheet.utils.withLtrIsolate
import com.stripe.android.testing.waitUntilWithIdle

internal class NfcScanningCardFormPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickOnNfcScan() {
        composeTestRule.waitUntilWithIdle {
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

    fun fillName(name: String) {
        composeTestRule.onNode(hasText("Name on card")).performTextReplacement(name)
        composeTestRule.waitForIdle()
    }

    fun assertScannedCardShown(
        lastFourDigits: String,
    ) {
        composeTestRule.waitUntilWithIdle {
            composeTestRule.onAllNodes(hasText("•••• $lastFourDigits".withLtrIsolate()))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("•••• $lastFourDigits".withLtrIsolate()).assertExists()
        composeTestRule.onNodeWithContentDescription(CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION).assertExists()
    }

    fun assertCvcIsFocused() {
        composeTestRule.waitUntilWithIdle {
            composeTestRule.onAllNodes(hasText("CVC").and(isFocused()))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    fun assertNameIsFocused() {
        composeTestRule.waitUntilWithIdle {
            composeTestRule.onAllNodes(hasText("Name on card").and(isFocused()))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION = "Clear scanned card"
    }
}

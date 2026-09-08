package com.stripe.android.paymentelement.nfcscan

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.stripe.android.common.taptoadd.TAP_TO_BUTTON_UI_TEST_TAG
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickNode
import com.stripe.android.testing.replaceText
import com.stripe.android.testing.waitForExactlyOneNode
import com.stripe.android.testing.waitForNode

internal class NfcScanningCardFormPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickOnNfcScan() {
        composeTestRule.waitForExactlyOneNode(
            matcher = hasTestTag(TAP_TO_BUTTON_UI_TEST_TAG),
            atLeastOneRootRequired = false,
        )

        val tapToAddButton = composeTestRule.onNode(hasTestTag(TAP_TO_BUTTON_UI_TEST_TAG))
        tapToAddButton.assertIsEnabled()
        composeTestRule.clickNode(
            node = tapToAddButton,
            scrollBehavior = ScrollBehavior.Never,
        )
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
            atLeastOneRootRequired = false,
        )

        composeTestRule.onNodeWithText("•••• $lastFourDigits").assertExists()
        composeTestRule.onNodeWithContentDescription(CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION).assertExists()
    }

    fun assertCvcIsFocused() {
        composeTestRule.waitForNode(
            matcher = hasText("CVC").and(isFocused()),
            atLeastOneRootRequired = false,
        )
    }

    private companion object {
        const val CLEAR_SCANNED_CARD_CONTENT_DESCRIPTION = "Clear scanned card"
    }
}

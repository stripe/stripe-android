package com.stripe.android.customersheet

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.PAYMENT_OPTION_CARD_TEST_TAG

internal class CustomerSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickPaymentOptionItem(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasText(text, ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("${PAYMENT_OPTION_CARD_TEST_TAG}_$text")
            .performClick()

        composeTestRule.waitForIdle()
    }

    fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasText(text, ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun waitForTextExactly(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTextExactly(text))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}

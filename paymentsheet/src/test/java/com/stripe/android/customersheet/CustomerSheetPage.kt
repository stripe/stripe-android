package com.stripe.android.customersheet

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.ui.SHEET_NAVIGATION_BUTTON_TAG

internal class CustomerSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickNavigationIcon() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(SHEET_NAVIGATION_BUTTON_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(SHEET_NAVIGATION_BUTTON_TAG)
            .performClick()

        composeTestRule.waitForIdle()
    }

    fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasText(text))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}

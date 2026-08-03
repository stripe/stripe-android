package com.stripe.android.test.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT

class PaymentSelection(val composeTestRule: ComposeTestRule, val paymentMethodCode: String) {
    fun click() {
        // The horizontal payment-method tab list (TEST_TAG_LIST) only exists in horizontal layout
        // mode; in vertical mode selection happens elsewhere, so a missing list is a legitimate
        // no-op rather than a race. atLeastOneRootRequired = false keeps this from throwing if no
        // compose root is momentarily attached mid-transition.
        val hasTabList = composeTestRule.onAllNodes(hasTestTag(TEST_TAG_LIST))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
        if (!hasTabList) {
            return
        }

        val paymentMethodMatcher = hasTestTag(TEST_TAG_LIST + paymentMethodCode)

        composeTestRule.onNodeWithTag(TEST_TAG_LIST, true)
            .performScrollToNode(paymentMethodMatcher)

        // Wait for the target tab to be present and enabled before clicking. The row can exist in
        // the tree but not yet be hittable while the list is still settling, which previously made
        // the one-shot assertIsDisplayed()/assertIsEnabled() flake.
        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule.onAllNodes(paymentMethodMatcher.and(isEnabled()))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeTestRule
            .onNode(paymentMethodMatcher)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
    }
}

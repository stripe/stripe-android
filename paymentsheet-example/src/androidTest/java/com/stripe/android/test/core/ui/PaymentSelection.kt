package com.stripe.android.test.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST

class PaymentSelection(val composeTestRule: ComposeTestRule, val paymentMethodCode: String) {
    fun click() {
        if (composeTestRule.onAllNodes(hasTestTag(TEST_TAG_LIST)).fetchSemanticsNodes().isNotEmpty()) {
            val paymentMethodMatcher = hasTestTag(TEST_TAG_LIST + paymentMethodCode)

            composeTestRule.onNodeWithTag(TEST_TAG_LIST, true)
                .performScrollToNode(paymentMethodMatcher)
            composeTestRule.waitForIdle()
            composeTestRule
                .onNode(paymentMethodMatcher)
                .assertIsDisplayed()
                .assertIsEnabled()
                .performClick()

            composeTestRule.waitForIdle()
        }
    }
}

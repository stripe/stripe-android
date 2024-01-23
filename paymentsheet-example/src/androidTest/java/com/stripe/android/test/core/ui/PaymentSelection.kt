package com.stripe.android.test.core.ui

import androidx.annotation.StringRes
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.paymentsheet.TEST_TAG_LIST
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT

class PaymentSelection(val composeTestRule: ComposeTestRule, @StringRes val label: Int) {
    fun click() {
        val resource = InstrumentationRegistry.getInstrumentation().targetContext.resources

        try {
            // If we don't find the node, it means that there's only one payment method available
            // and we don't show the payment method carousel as a result.
            composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                composeTestRule
                    .onAllNodesWithTag(TEST_TAG_LIST)
                    .fetchSemanticsNodes().size == 1
            }
        } catch (_: ComposeTimeoutException) {
            return
        }

        composeTestRule.onNodeWithTag(TEST_TAG_LIST, true)
            .performScrollToNode(hasText(resource.getString(label)))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(TEST_TAG_LIST + resource.getString(label))
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
    }
}

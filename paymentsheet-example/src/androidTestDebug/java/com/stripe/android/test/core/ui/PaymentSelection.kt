package com.stripe.android.test.core.ui

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.paymentsheet.TEST_TAG_LIST

class PaymentSelection(val composeTestRule: ComposeTestRule, @StringRes val label: Int) {
    fun click() {
        val resource = InstrumentationRegistry.getInstrumentation().targetContext.resources
        composeTestRule.onNodeWithTag(TEST_TAG_LIST, true)
            .performScrollToNode(hasText(resource.getString(label)))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(resource.getString(label))
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
    }
}

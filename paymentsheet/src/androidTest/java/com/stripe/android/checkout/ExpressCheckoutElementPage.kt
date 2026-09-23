package com.stripe.android.checkout

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.testing.waitUntilWithIdle

internal class ExpressCheckoutElementPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickGooglePayButton() {
        clickButton(
            testTag = GOOGLE_PAY_BUTTON_TEST_TAG,
            name = "Google Pay",
        )
    }

    fun clickLinkButton() {
        clickButton(
            testTag = LinkButtonTestTag,
            name = "Link",
        )
    }

    fun assertGooglePayButtonExists() {
        composeTestRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertExists()
    }

    fun assertLinkButtonDoesNotExist() {
        composeTestRule.onNodeWithTag(LinkButtonTestTag).assertDoesNotExist()
    }

    private fun clickButton(testTag: String, name: String) {
        val button = hasTestTag(testTag) and isEnabled() and hasClickAction()
        composeTestRule.waitUntilWithIdle(
            conditionDescription = "$name button is enabled and clickable",
        ) {
            composeTestRule.onAllNodes(button)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeTestRule.onNode(button).performClick()
    }
}

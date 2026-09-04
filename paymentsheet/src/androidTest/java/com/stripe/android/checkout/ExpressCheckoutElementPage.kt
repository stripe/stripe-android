package com.stripe.android.checkout

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG

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

    private fun clickButton(testTag: String, name: String) {
        val button = hasTestTag(testTag) and isEnabled() and hasClickAction()
        composeTestRule.waitUntil(
            conditionDescription = "$name button is enabled and clickable",
            timeoutMillis = 5_000,
        ) {
            composeTestRule.onAllNodes(button)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeTestRule.onNode(button).performClick()
    }
}

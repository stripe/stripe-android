package com.stripe.android.checkout

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.testing.waitForNode

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
        composeTestRule.waitForNode(
            matcher = button,
            atLeastOneRootRequired = false,
            conditionDescription = "$name button is enabled and clickable",
        )

        composeTestRule.onNode(button).performClick()
    }
}

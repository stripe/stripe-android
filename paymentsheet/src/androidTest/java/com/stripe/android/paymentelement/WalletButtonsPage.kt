package com.stripe.android.paymentelement

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG

class WalletButtonsPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun assertLinkIsDisplayed() {
        composeTestRule.onNode(hasTestTag(LinkButtonTestTag)).assertIsDisplayed()
    }

    fun assertGooglePayIsDisplayed() {
        composeTestRule.onNode(hasTestTag(GOOGLE_PAY_BUTTON_TEST_TAG)).assertIsDisplayed()
    }
}

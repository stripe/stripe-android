package com.stripe.android.paymentelement.taptoadd

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.stripe.android.common.taptoadd.TAP_TO_BUTTON_UI_TEST_TAG

class TapToAddCardFormPage(
    val composeTestRule: ComposeTestRule
) {
    fun clickOnTapToAdd() {
        val buttonMatcher = hasTestTag(TAP_TO_BUTTON_UI_TEST_TAG)

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(buttonMatcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1

        }

        composeTestRule.onNode(buttonMatcher)
            .assertIsEnabled()
            .performClick()
    }
}

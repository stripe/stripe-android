package com.stripe.android.paymentelement.taptoadd

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick

class TapToAddCardFormPage(
    val composeTestRule: ComposeTestRule
) {
    fun clickOnTapToAdd() {
        val buttonMatcher = hasText(TAP_TO_ADD_BUTTON_TEXT)

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(buttonMatcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1

        }

        composeTestRule.onNode(buttonMatcher)
            .assertIsEnabled()
            .performClick()
    }

    private companion object {
        const val TAP_TO_ADD_BUTTON_TEXT = "Tap to add"
    }
}

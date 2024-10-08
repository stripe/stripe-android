package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON

class RemoveDialog(private val composeTestRule: ComposeTestRule) {
    fun confirm(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag(TEST_TAG_DIALOG_CONFIRM_BUTTON)
        ).performClick()
    }
}

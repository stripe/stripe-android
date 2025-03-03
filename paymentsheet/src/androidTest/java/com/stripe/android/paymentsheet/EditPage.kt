package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SAVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SCREEN_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG

internal class EditPage(
    private val composeTestRule: ComposeTestRule
) {
    fun waitUntilVisible() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(UPDATE_PM_SCREEN_TEST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun update(waitUntilComplete: Boolean = true) {
        composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)
            .performClick()
        if (waitUntilComplete) {
            composeTestRule.waitUntil(timeoutMillis = 5_000L) {
                composeTestRule
                    .onAllNodes(hasTestTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).and(hasTestMetadata("isLoading=true")))
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        }
    }

    fun clickSetAsDefaultCheckbox() {
        composeTestRule.onNodeWithTag(
            UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG
        ).performClick()
    }
}

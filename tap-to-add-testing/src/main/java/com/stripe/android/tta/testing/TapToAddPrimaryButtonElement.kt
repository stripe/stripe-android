package com.stripe.android.tta.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG

class TapToAddPrimaryButtonElement(
    private val composeTestRule: ComposeTestRule
) {
    fun assert(withLabel: String?): SemanticsNodeInteraction {
        val matcher = hasTestTag(PRIMARY_BUTTON_TEST_TAG)
            .and(hasClickAction())
            .run {
                withLabel?.let {
                    and(hasText(withLabel))
                } ?: this
            }

        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule
                .onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }

        return composeTestRule.onNode(matcher)
            .assertExists()
    }

    fun assertNotShown() {
        composeTestRule.onNode(hasTestTag(PRIMARY_BUTTON_TEST_TAG))
            .assertDoesNotExist()
    }
}

internal fun SemanticsNodeInteraction.click() {
    assertIsEnabled()
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
}

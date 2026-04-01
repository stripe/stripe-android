package com.stripe.android.common.taptoadd

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
    fun assert(label: String): SemanticsNodeInteraction {
        val matcher = hasTestTag(PRIMARY_BUTTON_TEST_TAG)
            .and(hasText(label))
            .and(hasClickAction())

        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule
                .onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        return composeTestRule.onNode(matcher)
            .assertExists()
    }

    private companion object {
        const val DEFAULT_UI_TIMEOUT = 5000L
    }
}

internal fun SemanticsNodeInteraction.click() {
    assertIsEnabled()
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
}

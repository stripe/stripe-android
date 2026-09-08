package com.stripe.android.tta.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.testing.waitForExactlyOneNode

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

        composeTestRule.waitForExactlyOneNode(
            matcher = matcher,
            atLeastOneRootRequired = false,
        )

        return composeTestRule.onNode(matcher)
            .assertExists()
    }

    fun assertNotShown() {
        composeTestRule.onNode(hasTestTag(PRIMARY_BUTTON_TEST_TAG))
            .assertDoesNotExist()
    }
}

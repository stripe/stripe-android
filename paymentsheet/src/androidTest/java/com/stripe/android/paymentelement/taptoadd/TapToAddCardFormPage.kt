package com.stripe.android.paymentelement.taptoadd

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.stripe.android.testing.waitForExactlyOneNode

class TapToAddCardFormPage(
    val composeTestRule: ComposeTestRule
) {
    fun clickOnTapToAdd() {
        val buttonMatcher = hasText(TAP_TO_ADD_BUTTON_TEXT)

        composeTestRule.waitForExactlyOneNode(
            matcher = buttonMatcher,
            timeoutMillis = 5_000,
            atLeastOneRootRequired = false,
        )

        composeTestRule.onNode(buttonMatcher)
            .assertIsEnabled()
            .performClick()
    }

    private companion object {
        const val TAP_TO_ADD_BUTTON_TEXT = "Tap to add"
    }
}

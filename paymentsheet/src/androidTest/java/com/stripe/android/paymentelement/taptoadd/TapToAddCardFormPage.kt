package com.stripe.android.paymentelement.taptoadd

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickNode
import com.stripe.android.testing.waitForExactlyOneNode

class TapToAddCardFormPage(
    val composeTestRule: ComposeTestRule
) {
    fun clickOnTapToAdd() {
        val buttonMatcher = hasText(TAP_TO_ADD_BUTTON_TEXT)

        composeTestRule.waitForExactlyOneNode(
            matcher = buttonMatcher,
            atLeastOneRootRequired = false,
        )

        val button = composeTestRule.onNode(buttonMatcher)
        button.assertIsEnabled()
        composeTestRule.clickNode(
            node = button,
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    private companion object {
        const val TAP_TO_ADD_BUTTON_TEXT = "Tap to add"
    }
}

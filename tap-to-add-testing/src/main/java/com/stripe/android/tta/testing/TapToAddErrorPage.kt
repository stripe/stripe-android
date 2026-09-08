package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickEnabledNode
import com.stripe.android.testing.waitForDisplayedNode

class TapToAddErrorPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun assertShown(
        expectedTitle: String,
        expectedAction: String
    ) {
        composeTestRule.waitForDisplayedNode(matcher = hasText(expectedTitle))
        composeTestRule.onNode(hasText(expectedTitle)).assertIsDisplayed()
        composeTestRule.onNode(hasText(expectedAction)).assertIsDisplayed()
        composeTestRule.retrieveCloseButton().assertIsDisplayed().assertIsEnabled()
    }

    fun clickCloseButton() {
        composeTestRule.clickEnabledNode(
            node = composeTestRule.retrieveCloseButton(),
            scrollBehavior = ScrollBehavior.Required,
        )
    }
}

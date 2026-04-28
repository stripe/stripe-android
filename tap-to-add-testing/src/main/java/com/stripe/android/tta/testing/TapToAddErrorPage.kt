package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule

class TapToAddErrorPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun assertShown(
        expectedTitle: String,
        expectedAction: String
    ) {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule.onNode(hasText(expectedTitle)).isDisplayed()
        }
        composeTestRule.onNode(hasText(expectedTitle)).assertIsDisplayed()
        composeTestRule.onNode(hasText(expectedAction)).assertIsDisplayed()
        composeTestRule.retrieveCloseButton().assertIsDisplayed().assertIsEnabled()
    }

    fun clickCloseButton() {
        composeTestRule.retrieveCloseButton().click()
    }
}

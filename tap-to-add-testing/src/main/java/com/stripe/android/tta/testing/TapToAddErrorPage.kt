package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule

class TapToAddErrorPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun assertShown(errorMessage: String) {
        assertHasErrorTitle()
        composeTestRule.onNode(hasText(errorMessage)).isDisplayed()
        composeTestRule.retrieveCloseButton().assertIsDisplayed().assertIsEnabled()
    }

    fun clickCloseButton() {
        composeTestRule.retrieveCloseButton().click()
    }

    private fun assertHasErrorTitle() {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule.onNode(hasText("Error")).isDisplayed()
        }
    }
}

package com.stripe.android.tta.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule

class TapToAddErrorPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun assertShown(errorMessage: String) {
        assertHasErrorTitle()
        composeTestRule.onNode(hasText(errorMessage)).isDisplayed()
        retrieveCloseButton().assertIsDisplayed().assertIsEnabled()
    }

    fun clickCloseButton() {
        retrieveCloseButton().click()
    }

    private fun retrieveCloseButton(): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasContentDescription("Close"))
    }

    private fun assertHasErrorTitle() {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule.onNode(hasText("Error")).isDisplayed()
        }
    }
}

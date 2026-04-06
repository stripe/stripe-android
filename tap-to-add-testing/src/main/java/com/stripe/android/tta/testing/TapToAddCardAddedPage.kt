package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule

class TapToAddCardAddedPage(
    private val composeTestRule: ComposeTestRule,
    private val linkHelper: TapToAddLinkTestHelper,
) {
    private val primaryButtonElement = TapToAddPrimaryButtonElement(composeTestRule)

    fun assertShown(withLink: Boolean) {
        assertHasCardAddedText()

        if (withLink) {
            linkHelper.checkbox().assertExists()
        }

        assertHasContinueButton()
    }

    fun clickCheckboxToSaveWithLink() {
        linkHelper.checkbox().click()
    }

    fun fillLinkInput() {
        linkHelper.fillEmail()
        linkHelper.fillPhone()
        linkHelper.fillName()
    }

    fun assertContinueButton(isEnabled: Boolean) {
        primaryButtonElement.assert(label = "Continue").run {
            if (isEnabled) {
                assertIsEnabled()
            } else {
                assertIsNotEnabled()
            }
        }
    }

    fun clickContinue() {
        assertHasContinueButton().click()
    }

    private fun assertHasCardAddedText() {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule.onNode(hasText("Card added")).isDisplayed()
        }
    }

    private fun assertHasContinueButton() = primaryButtonElement.assert(label = "Continue")
}

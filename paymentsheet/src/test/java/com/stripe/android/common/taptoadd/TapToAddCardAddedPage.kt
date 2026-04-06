package com.stripe.android.common.taptoadd

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule

internal class TapToAddCardAddedPage(
    private val composeTestRule: ComposeTestRule,
    private val linkHelper: TapToAddLinkHelper,
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
        composeTestRule.waitUntil(5000L) {
            composeTestRule.onNode(hasText("Card added")).isDisplayed()
        }
    }

    private fun assertHasContinueButton() = primaryButtonElement.assert(label = "Continue")
}

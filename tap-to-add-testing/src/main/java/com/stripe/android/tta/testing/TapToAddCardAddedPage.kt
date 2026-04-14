package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_CARD_ADDED_SHOWN_DELAY

class TapToAddCardAddedPage(
    private val composeTestRule: ComposeTestRule,
    private val linkHelper: TapToAddLinkTestHelper,
) {
    private val primaryButtonElement = TapToAddPrimaryButtonElement(composeTestRule)

    fun assertShown(
        withLink: Boolean = false,
    ) {
        assertHasCardAddedText()

        if (withLink) {
            linkHelper.checkbox().assertExists()
        }

        if (withLink) {
            assertHasContinueButton()
        } else {
            primaryButtonElement.assertNotShown()
        }
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
        assertHasContinueButton().run {
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

    fun clickCloseButton() {
        composeTestRule.retrieveCloseButton().click()
    }

    fun advancePastScreen() {
        composeTestRule.mainClock.advanceTimeBy(TAP_TO_ADD_CARD_ADDED_SHOWN_DELAY)
    }

    fun waitUntilMissing() {
        composeTestRule.waitUntilLayoutWithPrimaryButtonMissing()
    }

    private fun assertHasCardAddedText() {
        val matcher = hasText("Card added")

        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule.onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }

        composeTestRule.onNode(matcher).isDisplayed()
    }

    private fun assertHasContinueButton() = primaryButtonElement.assert(withLabel = "Continue")
}

package com.stripe.android.tta.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput

class TapToAddConfirmationPage(
    private val composeTestRule: ComposeTestRule,
) {
    private val primaryButtonElement = TapToAddPrimaryButtonElement(composeTestRule)

    fun assertPrimaryButton(withLabel: String? = null, isEnabled: Boolean = true) {
        primaryButtonElement.assert(withLabel).run {
            if (isEnabled) {
                assertIsEnabled()
            } else {
                assertIsNotEnabled()
            }
        }
    }

    fun assertErrorMessageShown(message: String) {
        composeTestRule.onNode(hasText(message)).assertIsDisplayed()
    }

    fun assertCvcRecollectionFieldShown() {
        retrieveCvcField()
            .assertExists()
            .assertIsEnabled()
    }

    fun fillCvc(cvc: String) {
        retrieveCvcField()
            .performScrollTo()
            .performTextInput(cvc)
    }

    fun clickPrimaryButton() {
        primaryButtonElement.assert(null).click()
    }

    fun waitUntilMissing() {
        composeTestRule.waitUntilLayoutWithPrimaryButtonMissing()
    }

    fun clickCloseButton() {
        composeTestRule.retrieveCloseButton().click()
    }

    private fun retrieveCvcField(): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasText("CVC"))
    }
}

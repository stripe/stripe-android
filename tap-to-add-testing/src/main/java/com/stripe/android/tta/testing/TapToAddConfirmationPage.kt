package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextInput

class TapToAddConfirmationPage(
    private val composeTestRule: ComposeTestRule,
) {
    private val primaryButtonElement = TapToAddPrimaryButtonElement(composeTestRule)

    fun assertPrimaryButton(label: String, isEnabled: Boolean) {
        primaryButtonElement.assert(label).run {
            if (isEnabled) {
                assertIsEnabled()
            } else {
                assertIsNotEnabled()
            }
        }
    }

    fun assertCvcRecollectionFieldShown() {
        composeTestRule.onNode(hasText("CVC"))
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    fun fillCvc(cvc: String) {
        composeTestRule.onNode(hasText("CVC"))
            .performTextInput(cvc)
    }

    fun clickPrimaryButton(label: String) {
        primaryButtonElement.assert(label).click()
    }
}

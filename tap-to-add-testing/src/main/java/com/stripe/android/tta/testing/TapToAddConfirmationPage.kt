package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule

class TapToAddConfirmationPage(
    composeTestRule: ComposeTestRule,
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

    fun clickPrimaryButton(label: String) {
        primaryButtonElement.assert(label).click()
    }
}

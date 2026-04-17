package com.stripe.android.tta.testing

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule

class TapToAddSavedPaymentMethodPage(
    private val composeTestRule: ComposeTestRule,
    val linkHelper: TapToAddLinkTestHelper,
) {
    fun assertShown() {
        assertHasAddedCardText()
        linkHelper.checkbox().assertExists()
    }

    fun waitUntilMissing() {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodes(hasText(TITLE))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    fun fillLink() {
        linkHelper.checkbox().click()
        linkHelper.fillEmail()
        linkHelper.fillPhone()
    }

    private fun assertHasAddedCardText() {
        val matcher = hasText(TITLE)

        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT) {
            composeTestRule.onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }

        composeTestRule.onNode(matcher).isDisplayed()
    }

    private companion object {
        const val TITLE = "Added card"
    }
}

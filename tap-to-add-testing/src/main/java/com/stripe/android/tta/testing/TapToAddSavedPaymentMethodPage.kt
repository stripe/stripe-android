package com.stripe.android.tta.testing

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.testing.waitUntilWithIdle

class TapToAddSavedPaymentMethodPage(
    private val composeTestRule: ComposeTestRule,
    val linkHelper: TapToAddLinkTestHelper,
) {
    fun assertShown() {
        assertHasAddedCardText()
        linkHelper.checkbox().assertExists()
    }

    fun waitUntilMissing() {
        composeTestRule.waitUntilWithIdle {
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

        composeTestRule.waitUntilWithIdle {
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

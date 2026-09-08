package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickEnabledNode
import com.stripe.android.testing.waitForExactlyOneNode
import com.stripe.android.testing.waitForNoNodes

class TapToAddSavedPaymentMethodPage(
    private val composeTestRule: ComposeTestRule,
    val linkHelper: TapToAddLinkTestHelper,
) {
    fun assertShown() {
        assertHasAddedCardText()
        linkHelper.checkbox().assertExists()
    }

    fun waitUntilMissing() {
        composeTestRule.waitForNoNodes(
            matcher = hasText(TITLE),
            atLeastOneRootRequired = false,
        )
    }

    fun fillLink() {
        composeTestRule.clickEnabledNode(
            node = linkHelper.checkbox(),
            scrollBehavior = ScrollBehavior.Required,
        )
        linkHelper.fillEmail()
        linkHelper.fillPhone()
    }

    private fun assertHasAddedCardText() {
        val matcher = hasText(TITLE)

        composeTestRule.waitForExactlyOneNode(
            matcher = matcher,
            atLeastOneRootRequired = false,
        )

        composeTestRule.onNode(matcher).assertIsDisplayed()
    }

    private companion object {
        const val TITLE = "Added card"
    }
}

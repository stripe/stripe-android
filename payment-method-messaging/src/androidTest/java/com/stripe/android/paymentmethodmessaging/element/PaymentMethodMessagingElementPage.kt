package com.stripe.android.paymentmethodmessaging.element

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

@OptIn(ExperimentalTestApi::class)
class PaymentMethodMessagingElementPage(
    private val composeTestRule: ComposeTestRule
) {
    fun verifySinglePartner() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("single partner inline_partner_promotion", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Learn more").assertExists()
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("Klarna"),
            timeoutMillis = (5000L)
        )
        composeTestRule.onNodeWithContentDescription("Klarna").assertExists()
    }

    fun verifyMultiPartner() {
        composeTestRule.onNodeWithText("multi partner promotion", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Learn more").assertExists()
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("Klarna"),
            timeoutMillis = (5000L)
        )
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("Affirm"),
            timeoutMillis = (5000L)
        )
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("Cash App Afterpay"),
            timeoutMillis = (5000L)
        )
    }

    fun openAndCloseLearnMoreActivity() {
        composeTestRule.onNodeWithContentDescription("Learn more").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("close_button").assertExists()
        composeTestRule.onNodeWithTag("close_button").performClick()
        composeTestRule.waitForIdle()
    }
}

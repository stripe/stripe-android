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
    fun verifyNoContentDisplayed() {
        composeTestRule.onNodeWithText("Learn more", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText(
            text = "multi partner promotion",
            substring = true
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText(
            text = "single partner inline_partner_promotion",
            substring = true
        ).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Klarna").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Affirm").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Cash App Afterpay").assertDoesNotExist()
    }

    fun verifySinglePartner() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("single partner inline_partner_promotion", substring = true).assertExists()
        composeTestRule.onNodeWithText("Learn more", substring = true).assertExists()
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasContentDescription("Klarna"),
            timeoutMillis = (5000L)
        )
        composeTestRule.onNodeWithContentDescription("Klarna").assertExists()
    }

    fun verifyMultiPartner() {
        composeTestRule.onNodeWithText("multi partner promotion", substring = true).assertExists()
        composeTestRule.onNodeWithText("Learn more", substring = true).assertExists()
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

    fun verifyLegalDisclosure() {
        composeTestRule.onNodeWithText("18+, T&C apply. Credit subject to status.").assertExists()
    }

    fun openAndCloseLearnMoreActivity() {
        composeTestRule.onNodeWithText("Learn more", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("close_button").assertExists()
        composeTestRule.onNodeWithTag("close_button").performClick()
        composeTestRule.waitForIdle()
    }
}

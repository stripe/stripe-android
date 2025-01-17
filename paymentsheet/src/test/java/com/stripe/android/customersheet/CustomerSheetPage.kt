package com.stripe.android.customersheet

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.stripe.android.common.ui.performClickWithKeyboard
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG

internal class CustomerSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickOnText(text: String) {
        waitForText(text)

        composeTestRule.onNode(hasText(text, ignoreCase = true)).performClickWithKeyboard()
    }

    fun clickPaymentOptionItem(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasText(text, ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$text")
            .performClickWithKeyboard()

        composeTestRule.waitForIdle()
    }

    fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasText(text, ignoreCase = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun waitForTextExactly(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTextExactly(text))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun inputText(text: String, replacement: String) {
        composeTestRule.onNode(hasText(text)).performTextInput(replacement)
    }
}

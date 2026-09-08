package com.stripe.android.customersheet

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.testing.waitForNode

internal class CustomerSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickOnText(text: String) {
        waitForText(text)

        composeTestRule.onNode(hasText(text, ignoreCase = true)).performClick()
    }

    fun clickPaymentOptionItem(text: String) {
        waitForText(text)

        composeTestRule.onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$text")
            .performClick()

        composeTestRule.waitForIdle()
    }

    fun waitForText(text: String) {
        composeTestRule.waitForNode(
            matcher = hasText(text, ignoreCase = true),
            atLeastOneRootRequired = true,
        )
    }

    fun waitForTextExactly(text: String) {
        composeTestRule.waitForNode(
            matcher = hasTextExactly(text),
            atLeastOneRootRequired = true,
        )
    }

    fun inputText(text: String, replacement: String) {
        composeTestRule.onNode(hasText(text)).performTextInput(replacement)
    }
}

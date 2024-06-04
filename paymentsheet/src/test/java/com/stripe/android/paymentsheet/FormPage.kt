package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextReplacement

internal class FormPage(
    private val composeTestRule: ComposeTestRule,
) {
    val cardNumber: SemanticsNodeInteraction = nodeWithLabel("Card number")

    fun fillOutCardDetails(fillOutCardNumber: Boolean = true) {
        if (fillOutCardNumber) {
            replaceText(cardNumber, "4242424242424242")
        }
        replaceText("MM / YY", "12/34")
        replaceText("CVC", "123")
        replaceText("ZIP Code", "12345")
    }

    private fun replaceText(label: String, text: String) {
        composeTestRule.onNode(hasText(label))
            .performTextReplacement(text)
    }

    private fun replaceText(node: SemanticsNodeInteraction, text: String) {
        node
            .performTextReplacement(text)
    }

    private fun nodeWithLabel(label: String): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasText(label))
    }
}

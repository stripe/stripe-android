package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_TITLE

internal class FormPage(
    private val composeTestRule: ComposeTestRule,
) {
    val cardNumber: SemanticsNodeInteraction = nodeWithLabel("Card number")
    val title: SemanticsNodeInteraction = composeTestRule.onNodeWithTag(TEST_TAG_HEADER_TITLE)
    val headerIcon: SemanticsNodeInteraction = composeTestRule.onNodeWithTag(TEST_TAG_ICON_FROM_RES)

    fun fillOutCardDetails(fillOutCardNumber: Boolean = true) {
        waitUntilVisible()
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

    fun waitUntilVisible() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(FORM_ELEMENT_TEST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun assertIsNotDisplayed() {
        composeTestRule
            .onNodeWithTag(FORM_ELEMENT_TEST_TAG)
            .assertDoesNotExist()
    }
}

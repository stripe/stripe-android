package com.stripe.android.paymentelement

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.stripe.android.paymentelement.embedded.form.EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import kotlin.time.Duration.Companion.seconds

internal class EmbeddedFormPage(
    private val composeTestRule: ComposeTestRule,
) {
    val cardNumber: SemanticsNodeInteraction = nodeWithLabel("Card number")

    fun fillOutCardDetails(fillOutCardNumber: Boolean = true) {
        waitUntilVisible()
        if (fillOutCardNumber) {
            replaceText(cardNumber, "4242424242424242")
        }
        fillExpirationDate("12/34")
        replaceText("CVC", "123")
        replaceText("ZIP Code", "12345")
    }

    private fun replaceText(label: String, text: String) {
        composeTestRule.onNode(hasText(label))
            .performTextReplacement(text)
    }

    private fun fillExpirationDate(text: String) {
        composeTestRule.onNode(hasContentDescription(value = "Expiration date", substring = true))
            .performTextReplacement(text)
    }

    private fun replaceText(node: SemanticsNodeInteraction, text: String) {
        node.performTextReplacement(text)
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

    fun clickPrimaryButton() {
        composeTestRule.waitUntil {
            composeTestRule.onAllNodes(hasTestTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON).and(isEnabled()))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(5.seconds.inWholeMilliseconds) {
            composeTestRule.onAllNodesWithTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }

        composeTestRule.waitForIdle()
    }
}

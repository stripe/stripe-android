package com.stripe.paymentelementtestpages

import androidx.annotation.RestrictTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_TITLE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FormPage(
    private val composeTestRule: ComposeTestRule,
) {
    val cardNumber: SemanticsNodeInteraction = nodeWithLabel("Card number")
    val expirationDate: SemanticsNodeInteraction = composeTestRule.onNode(
        hasContentDescription(value = "Expiration date", substring = true)
    )
    val title: SemanticsNodeInteraction = composeTestRule.onNodeWithTag(TEST_TAG_HEADER_TITLE)
    val headerIcon: SemanticsNodeInteraction = composeTestRule.onNodeWithTag(TEST_TAG_ICON_FROM_RES)

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

    fun assertErrorExists(errorMessage: String) {
        composeTestRule.onNode(hasText(errorMessage)).assertExists()
    }

    fun fillCardNumber(number: String) {
        waitUntilVisible()
        replaceText(cardNumber, number)
    }

    fun fillOutName() {
        replaceText("Full name", "Jane Doe")
    }

    fun fillOutEmail() {
        replaceText("Email", "janedoe@example.com")
    }
}

fun SemanticsNodeInteraction.assertHasErrorMessage(expectedMessage: String) =
    assert(
        SemanticsMatcher("has error '$expectedMessage'") { node ->
            node.config[SemanticsProperties.Error] == expectedMessage
        }
    )

fun SemanticsNodeInteraction.assertHasNoErrorMessage() =
    assert(
        SemanticsMatcher("has no error ") { node ->
            node.config.contains(SemanticsProperties.Error).not()
        }
    )

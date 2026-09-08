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
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_TITLE
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.fillCardDetails
import com.stripe.android.testing.replaceText
import com.stripe.android.testing.waitForNoNodes
import com.stripe.android.testing.waitForNode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class FormPage(
    protected val composeTestRule: ComposeTestRule,
) {
    val cardNumber: SemanticsNodeInteraction = composeTestRule.onNode(hasText("Card number"))
    val expirationDate: SemanticsNodeInteraction = composeTestRule.onNode(
        hasContentDescription(value = "Expiration date", substring = true)
    )
    val title: SemanticsNodeInteraction = composeTestRule.onNodeWithTag(TEST_TAG_HEADER_TITLE)
    val headerIcon: SemanticsNodeInteraction = composeTestRule.onNodeWithTag(TEST_TAG_ICON_FROM_RES)

    fun fillOutCardDetails(fillOutCardNumber: Boolean = true) {
        fillOutCardDetails(
            newCardNumber = DEFAULT_CARD_NUMBER,
            fillOutCardNumber = fillOutCardNumber,
        )
    }

    fun fillOutCardDetails(
        newCardNumber: String,
        fillOutCardNumber: Boolean = true,
    ) {
        waitUntilVisible()
        composeTestRule.fillCardDetails(
            cardNumber = newCardNumber.takeIf { fillOutCardNumber },
            expirationDate = "12/34",
            cvc = "123",
            zipCode = "12345",
            textFieldScrollBehavior = ScrollBehavior.Never,
        )
    }

    fun waitUntilVisible() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(FORM_ELEMENT_TEST_TAG),
            atLeastOneRootRequired = false,
        )
    }

    fun waitUntilMissing() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(FORM_ELEMENT_TEST_TAG),
            atLeastOneRootRequired = false,
        )
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
        composeTestRule.replaceText(cardNumber, number)
    }

    fun fillOutName() {
        composeTestRule.replaceText(
            matcher = hasText("Full name"),
            text = "Jane Doe",
        )
    }

    fun fillOutEmail() {
        composeTestRule.replaceText(
            matcher = hasText("Email"),
            text = "janedoe@example.com",
        )
    }

    private companion object {
        const val DEFAULT_CARD_NUMBER = "4242424242424242"
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

package com.stripe.paymentelementtestpages

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.stripe.android.paymentelement.embedded.linkinline.EMBEDDED_LINK_INLINE_CARD_CLOSE_TEST_TAG
import com.stripe.android.paymentelement.embedded.linkinline.EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG
import com.stripe.android.paymentelement.embedded.linkinline.EMBEDDED_LINK_INLINE_CARD_TEST_TAG

/**
 * The card a returning Link customer verifies in, shown in place of the Link row in the Embedded Payment Element,
 * and the Link dialog it can enter the code in instead.
 */
class EmbeddedLinkInlineVerificationPage(
    private val composeTestRule: ComposeTestRule,
    /**
     * How long to wait for each step. Tests against a live backend need longer than mocked ones.
     */
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    fun waitUntilVisible() {
        waitUntilExists(hasTestTag(EMBEDDED_LINK_INLINE_CARD_TEST_TAG), "inline Link verification card to appear")
    }

    fun waitUntilMissing() {
        waitUntil("inline Link verification card to go away") {
            nodes(hasTestTag(EMBEDDED_LINK_INLINE_CARD_TEST_TAG)).isEmpty()
        }
    }

    fun assertIsNotVisible() {
        composeTestRule.onNode(hasTestTag(EMBEDDED_LINK_INLINE_CARD_TEST_TAG)).assertDoesNotExist()
    }

    /**
     * Waits for the code to be sent, which enables the code.
     */
    fun waitUntilCodeIsSent() {
        waitUntilExists(hasTestTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG).and(isEnabled()), "the code to be sent")
    }

    fun tapCode() {
        composeTestRule.onNode(hasTestTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG))
            .performScrollTo()
            .performClick()
    }

    /**
     * Types [code] in the card, once the tapped code took focus in place.
     */
    fun typeCode(code: String) {
        val firstDigit = hasTestTag(FIRST_DIGIT_TEST_TAG)
            .and(hasAnyAncestor(hasTestTag(EMBEDDED_LINK_INLINE_CARD_TEST_TAG)))
        waitUntilExists(firstDigit.and(isFocused()), "the card's code to take focus")
        composeTestRule.onNode(firstDigit).performTextInput(code)
    }

    /**
     * Enters [code] in the Link dialog, once it takes a code.
     */
    fun enterCodeInDialog(code: String) {
        val firstDigit = hasTestTag(FIRST_DIGIT_TEST_TAG).and(hasAnyAncestor(isDialog()))
        waitUntilExists(firstDigit.and(isEnabled()), "the Link dialog to take a code")
        composeTestRule.onNode(firstDigit).performTextInput(code)
    }

    fun close() {
        composeTestRule.onNode(hasTestTag(EMBEDDED_LINK_INLINE_CARD_CLOSE_TEST_TAG))
            .performScrollTo()
            .performClick()
    }

    private fun waitUntilExists(matcher: SemanticsMatcher, conditionDescription: String) {
        waitUntil(conditionDescription) { nodes(matcher).isNotEmpty() }
    }

    private fun waitUntil(conditionDescription: String, condition: () -> Boolean) {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(conditionDescription, timeoutMillis, condition)
    }

    private fun nodes(matcher: SemanticsMatcher) = composeTestRule.onAllNodes(matcher)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 5_000L

        /**
         * The first box of a code, which takes a whole pasted code.
         */
        private const val FIRST_DIGIT_TEST_TAG = "OTP-0"
    }
}

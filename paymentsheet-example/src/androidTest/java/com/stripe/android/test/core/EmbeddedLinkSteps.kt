package com.stripe.android.test.core

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.paymentelementtestpages.EmbeddedLinkInlineVerificationPage

/**
 * Steps for paying with Link in the Embedded Payment Element playground, for a returning Link customer.
 */
internal class EmbeddedLinkSteps(
    private val composeTestRule: ComposeTestRule,
) {
    val inlineVerification = EmbeddedLinkInlineVerificationPage(
        composeTestRule = composeTestRule,
        timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
    )

    private val linkRow = hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_link")

    fun selectLink() {
        composeTestRule.onNode(linkRow)
            .performScrollTo()
            .performClick()
    }

    fun waitUntilLinkIsSelected() {
        waitUntilExists(linkRow.and(isSelected()), "Link to be selected")
    }

    /**
     * Presses the Link wallet's primary button. It picks what to pay with when the wallet opened to pick, and pays
     * otherwise.
     */
    fun pressLinkWalletButton() {
        val button = hasTestTag(LINK_WALLET_BUTTON_TEST_TAG)
        waitUntilExists(button.and(isEnabled()), "the Link wallet's button to be enabled")
        composeTestRule.onNode(button)
            .performScrollTo()
            .performClick()
    }

    private fun waitUntilExists(matcher: SemanticsMatcher, conditionDescription: String) {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(conditionDescription, DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule.onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val LINK_WALLET_BUTTON_TEST_TAG = "wallet_screen_pay_button"
    }
}

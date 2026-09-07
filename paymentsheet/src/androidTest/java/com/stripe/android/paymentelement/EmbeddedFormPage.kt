package com.stripe.android.paymentelement

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.stripe.android.paymentsheet.ui.SHEET_ERROR_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_PROMO_BADGE
import com.stripe.android.testing.waitForNode
import com.stripe.paymentelementtestpages.FormPage
import kotlin.time.Duration.Companion.seconds

internal class EmbeddedFormPage(
    composeTestRule: ComposeTestRule,
) : FormPage(composeTestRule) {
    val cardNumberText: SemanticsNodeInteraction
        get() = cardNumber

    fun clickPrimaryButton() {
        clickPrimaryButtonWithoutWaitingForDismissal()

        composeTestRule.waitUntil(5.seconds.inWholeMilliseconds) {
            composeTestRule.onAllNodesWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }

        composeTestRule.waitForIdle()
    }

    fun clickDisabledPrimaryButton() {
        waitUntilVisible()

        composeTestRule.waitUntil(
            conditionDescription = "embedded form primary button to become disabled",
            timeoutMillis = 5.seconds.inWholeMilliseconds,
        ) {
            composeTestRule.onAllNodes(
                hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(isNotEnabled())
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG)
            .performScrollTo()
            .performTouchInput { click() }

        composeTestRule.waitForIdle()
    }

    fun assertCardNumberError(errorMessage: String) {
        composeTestRule.waitUntil(
            conditionDescription = "card number field to show error '$errorMessage'",
            timeoutMillis = 5.seconds.inWholeMilliseconds,
        ) {
            composeTestRule.onAllNodes(
                hasText("Card number").and(
                    SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage)
                )
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun clickPrimaryButtonWithoutWaitingForDismissal() {
        waitUntilVisible()
        waitUntilPrimaryButtonIsEnabled()

        primaryButton()
            .performScrollTo()
            .performClick()
    }

    fun assertPrimaryButtonIsEnabled() {
        waitUntilPrimaryButtonIsEnabled()
        primaryButton().assertIsEnabled()
    }

    fun assertErrorIsShown(message: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag(SHEET_ERROR_TEST_TAG).and(hasText(message)))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNode(hasTestTag(SHEET_ERROR_TEST_TAG).and(hasText(message)))
            .performScrollTo()
            .assertIsDisplayed()
    }

    fun assertMandateIsShown() {
        waitUntilVisible()

        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG)
            .assertExists()
    }

    fun assertMandateIsMissing() {
        waitUntilVisible()

        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG)
            .assertDoesNotExist()
    }

    fun assertHeaderPromoBadgeIsDisplayed(text: String) {
        waitUntilVisible()

        val matcher = hasTestTag(TEST_TAG_HEADER_PROMO_BADGE).and(
            hasAnyDescendant(hasText(text, substring = true))
        )
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        composeTestRule.onNode(matcher, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    fun waitUntilHeaderPromoBadgeIsMissing() {
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(TEST_TAG_HEADER_PROMO_BADGE)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }

        composeTestRule.onNodeWithTag(TEST_TAG_HEADER_PROMO_BADGE)
            .assertDoesNotExist()
    }

    private fun waitUntilPrimaryButtonIsEnabled() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(isEnabled()),
            timeoutMillis = 1_000,
        )
    }

    private fun primaryButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG)
    }
}

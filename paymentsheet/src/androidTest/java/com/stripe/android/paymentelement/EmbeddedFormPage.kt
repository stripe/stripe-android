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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.stripe.android.paymentsheet.ui.SHEET_ERROR_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_PROMO_BADGE
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickNode
import com.stripe.android.testing.waitForNoNodes
import com.stripe.android.testing.waitForNode
import com.stripe.paymentelementtestpages.FormPage

internal class EmbeddedFormPage(
    composeTestRule: ComposeTestRule,
) : FormPage(composeTestRule) {
    val cardNumberText: SemanticsNodeInteraction
        get() = cardNumber

    fun clickPrimaryButton() {
        clickPrimaryButtonWithoutWaitingForDismissal()

        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG),
            atLeastOneRootRequired = false,
        )

        composeTestRule.waitForIdle()
    }

    fun clickDisabledPrimaryButton() {
        waitUntilVisible()

        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(isNotEnabled()),
            atLeastOneRootRequired = true,
            conditionDescription = "embedded form primary button to become disabled",
        )

        composeTestRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG)
            .performScrollTo()
            .performTouchInput { click() }

        composeTestRule.waitForIdle()
    }

    fun assertCardNumberError(errorMessage: String) {
        composeTestRule.waitForNode(
            matcher = hasText("Card number").and(
                SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage)
            ),
            atLeastOneRootRequired = true,
            conditionDescription = "card number field to show error '$errorMessage'",
        )
    }

    fun clickPrimaryButtonWithoutWaitingForDismissal() {
        waitUntilVisible()
        waitUntilPrimaryButtonIsEnabled()

        composeTestRule.clickNode(
            node = primaryButton(),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun assertPrimaryButtonIsEnabled() {
        waitUntilPrimaryButtonIsEnabled()
        primaryButton().assertIsEnabled()
    }

    fun assertErrorIsShown(message: String) {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_ERROR_TEST_TAG).and(hasText(message)),
            atLeastOneRootRequired = false,
        )
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
        composeTestRule.waitForNode(
            matcher = matcher,
            atLeastOneRootRequired = false,
            useUnmergedTree = true,
        )

        composeTestRule.onNode(matcher, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    fun waitUntilHeaderPromoBadgeIsMissing() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(TEST_TAG_HEADER_PROMO_BADGE),
            atLeastOneRootRequired = false,
        )

        composeTestRule.onNodeWithTag(TEST_TAG_HEADER_PROMO_BADGE)
            .assertDoesNotExist()
    }

    private fun waitUntilPrimaryButtonIsEnabled() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(isEnabled()),
            atLeastOneRootRequired = false,
        )
    }

    private fun primaryButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG)
    }
}

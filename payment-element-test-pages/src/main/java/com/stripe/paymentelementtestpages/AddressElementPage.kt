package com.stripe.paymentelementtestpages

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.waitUntilWithIdle
import androidx.compose.ui.R as ComposeUiR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings("TooManyFunctions")
class AddressElementPage(
    private val composeTestRule: ComposeTestRule,
    context: Context,
) {
    private val primaryButtonText = context.getString(R.string.stripe_paymentsheet_address_element_primary_button)
    private val primaryButton = composeTestRule.onNodeWithText(primaryButtonText)
    private val closeButton = composeTestRule.onNodeWithContentDescription(
        context.getString(R.string.stripe_paymentsheet_close)
    )
    private val scrim = composeTestRule.onNodeWithContentDescription(context.getString(ComposeUiR.string.close_sheet))

    fun waitUntilVisible() {
        waitForNode(hasText("Shipping Address"))
        waitForNode(hasText(primaryButtonText))
    }

    fun fillCompleteAddress() {
        replaceText("Full name", "Real Name")
        replaceText("Address line 1", "1234 Main St")
        replaceText("City", "Boston")
        replaceText("ZIP Code", "12345")
        composeTestRule.onNodeWithText("State").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Massachusetts").performScrollTo().performClick()
    }

    fun assertCompleteAddress() {
        assertTextFieldContains("Full name", "Real Name")
        assertTextFieldContains("Address line 1", "1234 Main St")
        assertTextFieldContains("City", "Boston")
        assertTextFieldContains("ZIP Code", "12345")
        composeTestRule.onNodeWithText("Massachusetts").performScrollTo().assertIsDisplayed()
    }

    fun enterAutocompleteQuery(query: String) {
        replaceText("Address", query)
    }

    fun selectAutocompletePrediction(primaryText: String) {
        waitForNode(hasText(primaryText))
        composeTestRule.onNodeWithText(primaryText).performClick()
    }

    fun clickSave() {
        waitForNode(hasText(primaryButtonText).and(isEnabled()))
        primaryButton.performScrollTo().assertIsEnabled().performClick()
    }

    fun clickDisabledSave() {
        waitForNode(hasText(primaryButtonText).and(isNotEnabled()))
        primaryButton.performScrollTo().performTouchInput { click() }
        composeTestRule.waitForIdle()
    }

    fun clickClose() {
        closeButton.performClick()
    }

    fun dismissViaScrimAccessibilityAction() {
        scrim.performSemanticsAction(SemanticsActions.OnClick)
    }

    fun assertReadyToSave() {
        primaryButton.assertIsEnabled()
        closeButton.assertIsEnabled()
    }

    fun assertSaving(loadingIndicatorTestTag: String) {
        primaryButton.assertIsNotEnabled()
        composeTestRule.onNodeWithTag(loadingIndicatorTestTag, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNode(
            matcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo).and(
                hasAnyAncestor(hasTestTag(loadingIndicatorTestTag))
            ),
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    fun assertRequiredFieldError() {
        val error = "This field cannot be blank."
        composeTestRule.waitUntilWithIdle(
            conditionDescription = "required-field validation error to appear",
        ) {
            composeTestRule.onAllNodesWithText(error)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onAllNodesWithText(error)[0].performScrollTo().assertIsDisplayed()
    }

    fun assertErrorDisplayed(error: String) {
        composeTestRule.waitUntilWithIdle(conditionDescription = "save error to appear") {
            composeTestRule.onAllNodesWithText(error)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(error).performScrollTo().assertIsDisplayed()
    }

    fun assertErrorNotDisplayed(error: String) {
        composeTestRule.waitUntilWithIdle(conditionDescription = "save error to disappear") {
            composeTestRule.onAllNodesWithText(error)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
        composeTestRule.onNodeWithText(error).assertDoesNotExist()
    }

    fun editName(name: String) {
        replaceText("Full name", name)
    }

    private fun replaceText(label: String, text: String) {
        val matcher = hasText(label).and(hasSetTextAction()).and(isEnabled())
        waitForNode(matcher)
        composeTestRule.onNode(matcher).performScrollTo().performTextReplacement(text)
    }

    private fun assertTextFieldContains(label: String, value: String) {
        composeTestRule.onNode(hasText(label).and(hasSetTextAction()))
            .performScrollTo()
            .assertTextContains(value)
    }

    private fun waitForNode(matcher: SemanticsMatcher) {
        composeTestRule.waitUntilWithIdle(conditionDescription = "node matching $matcher to appear") {
            composeTestRule.onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }
}

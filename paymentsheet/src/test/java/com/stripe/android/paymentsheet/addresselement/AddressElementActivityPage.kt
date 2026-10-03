package com.stripe.android.paymentsheet.addresselement

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.stripe.android.common.ui.PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_NAVIGATION_BUTTON_TAG
import com.stripe.android.testing.waitUntilWithIdle

internal class AddressElementActivityPage(
    private val composeTestRule: ComposeTestRule,
    primaryButtonText: String,
    scrimContentDescription: String,
) {
    private val primaryButton = composeTestRule.onNodeWithText(primaryButtonText)
    private val closeButton = composeTestRule.onNodeWithTag(SHEET_NAVIGATION_BUTTON_TAG)
    private val scrim = composeTestRule.onNodeWithContentDescription(scrimContentDescription)

    fun clickSave() {
        primaryButton.performScrollTo().assertIsEnabled().performClick()
    }

    fun clickClose() {
        closeButton.performClick()
    }

    fun dismissViaScrimAccessibilityAction() {
        scrim.performSemanticsAction(SemanticsActions.OnClick)
    }

    fun assertVisible() {
        primaryButton.performScrollTo().assertIsDisplayed()
        closeButton.assertIsDisplayed()
        scrim.assertIsDisplayed()
    }

    fun assertReadyToSave() {
        primaryButton.assertIsEnabled()
        closeButton.assertIsEnabled()
    }

    fun assertSaving() {
        primaryButton.assertIsNotEnabled()
        closeButton.assertIsNotEnabled()
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNode(
            matcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo).and(
                hasAnyAncestor(hasTestTag(PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG))
            ),
            useUnmergedTree = true,
        ).assertIsDisplayed()
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
        composeTestRule.onNode(hasText("Full name").and(hasSetTextAction()))
            .performScrollTo()
            .performTextReplacement(name)
    }
}

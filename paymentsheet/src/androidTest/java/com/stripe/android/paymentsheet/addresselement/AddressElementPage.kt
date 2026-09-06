package com.stripe.android.paymentsheet.addresselement

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import kotlin.time.Duration.Companion.seconds

internal class AddressElementPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun waitUntilVisible() {
        waitForText("Address")
        waitForText("Save address")
    }

    fun fillCompleteAddress() {
        replaceText("Full name", "Real Name")
        replaceText("Address line 1", "1234 Main St")
        replaceText("City", "Boston")
        replaceText("ZIP Code", "12345")
        composeTestRule.onNodeWithText("State").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Massachusetts").performScrollTo().performClick()
    }

    fun clickSave() {
        waitForNode(hasText("Save address").and(isEnabled()))
        composeTestRule.onNodeWithText("Save address").performScrollTo().performClick()
    }

    fun clickDisabledSave() {
        waitForNode(hasText("Save address").and(isNotEnabled()))
        composeTestRule.onNodeWithText("Save address")
            .performScrollTo()
            .performTouchInput { click() }
        composeTestRule.waitForIdle()
    }

    fun assertRequiredFieldError() {
        val error = "This field cannot be blank."
        composeTestRule.waitUntil(
            conditionDescription = "required-field validation error to appear",
            timeoutMillis = 5.seconds.inWholeMilliseconds,
        ) {
            composeTestRule.onAllNodesWithText(error)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onAllNodesWithText(error)[0]
            .performScrollTo()
            .assertIsDisplayed()
    }

    fun clickClose() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
    }

    private fun replaceText(label: String, text: String) {
        val matcher = hasText(label).and(hasSetTextAction())
        waitForNode(matcher)
        composeTestRule.onNode(matcher).performScrollTo().performTextReplacement(text)
    }

    private fun waitForText(text: String) {
        waitForNode(hasText(text))
    }

    private fun waitForNode(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(
            conditionDescription = "node matching $matcher to appear",
            timeoutMillis = 5.seconds.inWholeMilliseconds,
        ) {
            composeTestRule.onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }
}

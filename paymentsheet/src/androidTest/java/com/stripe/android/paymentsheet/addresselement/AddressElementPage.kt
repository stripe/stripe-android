package com.stripe.android.paymentsheet.addresselement

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import androidx.compose.ui.test.performTouchInput
import com.stripe.android.paymentsheet.utils.replaceText
import com.stripe.android.paymentsheet.utils.waitForNode
import com.stripe.android.paymentsheet.utils.waitForText
import kotlin.time.Duration.Companion.seconds

internal class AddressElementPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun waitUntilVisible() {
        composeTestRule.waitForText("Shipping Address")
        composeTestRule.waitForText("Save address")
    }

    fun fillCompleteAddress() {
        composeTestRule.replaceText("Full name", "Real Name")
        composeTestRule.replaceText("Address line 1", "1234 Main St")
        composeTestRule.replaceText("City", "Boston")
        composeTestRule.replaceText("ZIP Code", "12345")
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

    fun clickSave() {
        composeTestRule.waitForNode(hasText("Save address").and(isEnabled()))
        composeTestRule.onNodeWithText("Save address").performScrollTo().performClick()
    }

    fun clickDisabledSave() {
        composeTestRule.waitForNode(hasText("Save address").and(isNotEnabled()))
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

    private fun assertTextFieldContains(label: String, value: String) {
        composeTestRule.onNode(hasText(label).and(hasSetTextAction()))
            .performScrollTo()
            .assertTextContains(value)
    }
}

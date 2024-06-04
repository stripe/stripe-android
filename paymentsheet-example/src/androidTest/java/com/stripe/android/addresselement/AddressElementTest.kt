package com.stripe.android.addresselement

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.stripe.android.paymentsheet.example.samples.ui.addresselement.AddressElementExampleActivity
import com.stripe.android.paymentsheet.example.samples.ui.addresselement.SELECT_ADDRESS_BUTTON
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.android.utils.TestRules
import org.junit.Rule
import org.junit.Test

internal class AddressElementTest {
    @get:Rule
    val rules = TestRules.create()

    @get:Rule
    val activityRule = ActivityScenarioRule(AddressElementExampleActivity::class.java)

    @Test
    fun testAddressElement() {
        val selectAddressButton = ComposeButton(rules.compose, hasTestTag(SELECT_ADDRESS_BUTTON))
        selectAddressButton.waitForEnabled()
        selectAddressButton.click()
        replaceText("Full name", "Real Name")
        replaceText("Address line 1", "1234 Main St")
        replaceText("City", "Boston")
        replaceText("ZIP Code", "12345")
        rules.compose.onNodeWithText("State").performScrollTo().performClick()
        rules.compose.onNodeWithText("Massachusetts").performScrollTo().performClick()
        rules.compose.onNodeWithText("Save address").performScrollTo().performClick()
        val resultTextMatcher = hasText("Real Name\n1234 Main St\n\nBoston, MA 12345\nUS")
        rules.compose.waitUntil {
            rules.compose.onAllNodes(resultTextMatcher).fetchSemanticsNodes().isNotEmpty()
        }
        rules.compose.onNode(resultTextMatcher).assertIsDisplayed()
    }

    private fun replaceText(label: String, text: String) {
        rules.compose.onNodeWithText(label).performScrollTo().performTextReplacement(text)
    }
}

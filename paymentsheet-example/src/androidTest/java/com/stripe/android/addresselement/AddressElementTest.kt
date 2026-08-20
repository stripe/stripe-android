package com.stripe.android.addresselement

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.stripe.android.paymentsheet.example.samples.ui.addresselement.AddressElementExampleActivity
import com.stripe.android.paymentsheet.example.samples.ui.addresselement.DISABLE_GOOGLE_PLACES_AUTOCOMPLETE_EXTRA
import com.stripe.android.paymentsheet.example.samples.ui.addresselement.PUBLISHABLE_KEY_OVERRIDE_EXTRA
import com.stripe.android.paymentsheet.example.samples.ui.addresselement.SELECT_ADDRESS_BUTTON
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.android.utils.TestRules
import org.junit.Rule
import org.junit.Test

internal class AddressElementTest {
    @get:Rule
    val rules = TestRules.create {
        around(
            ActivityScenarioRule<AddressElementExampleActivity>(
                Intent(
                    ApplicationProvider.getApplicationContext(),
                    AddressElementExampleActivity::class.java,
                )
                    .putExtra(PUBLISHABLE_KEY_OVERRIDE_EXTRA, "pk_test_123")
                    .putExtra(DISABLE_GOOGLE_PLACES_AUTOCOMPLETE_EXTRA, true),
            )
        )
    }

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

package com.stripe.paymentelementtestpages

import androidx.annotation.RestrictTo
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.stripe.android.testing.waitUntilWithIdle

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BillingDetailsPage(
    private val composeTestRule: ComposeTestRule,
) {
    val country: SemanticsNodeInteraction = nodeWithLabel("Country or region")
    val line1: SemanticsNodeInteraction = nodeWithLabel("Address line 1")
    val line2: SemanticsNodeInteraction = nodeWithLabel("Address line 2 (optional)")
    val city: SemanticsNodeInteraction = nodeWithLabel("City")
    val zipCode: SemanticsNodeInteraction = nodeWithLabel("ZIP Code")
    val state: SemanticsNodeInteraction = nodeWithLabel("State")

    fun updateLine1(value: String) {
        line1.performScrollTo().performTextReplacement(value)
    }

    fun enterAddressManually(line1: String) {
        composeTestRule.waitUntilWithIdle(conditionDescription = "inline address field to be shown") {
            composeTestRule.onAllNodes(hasText("Address"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNode(hasText("Address")).performScrollTo().performTextReplacement(line1)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(
            conditionDescription = "'Enter address manually' to be shown",
            timeoutMillis = 15_000,
        ) {
            composeTestRule.onAllNodes(hasText("Enter address manually"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Enter address manually").performClick()
        composeTestRule.waitUntilWithIdle(conditionDescription = "expanded billing address form") {
            composeTestRule.onAllNodes(hasText("ZIP Code"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    fun fillOutUnitedStatesAddress(
        line1: String,
        city: String,
        stateName: String,
        zipCode: String,
    ) {
        country.assertTextContains("United States")
        this.line1.performTextReplacement(line1)
        fillOutUnitedStatesCityStateAndZip(city, stateName, zipCode)
    }

    fun fillOutUnitedStatesCityStateAndZip(
        city: String,
        stateName: String,
        zipCode: String,
    ) {
        this.city.performTextReplacement(city)
        state.performScrollTo().performClick()
        composeTestRule.onNodeWithText(stateName).performScrollTo().performClick()
        this.zipCode.performTextReplacement(zipCode)
    }

    private fun nodeWithLabel(label: String): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasText(label))
    }
}

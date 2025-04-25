package com.stripe.paymentelementtestpages

import androidx.annotation.RestrictTo
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

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

    private fun nodeWithLabel(label: String): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasText(label))
    }
}

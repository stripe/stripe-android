package com.stripe.android.test.core.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import com.stripe.android.R as StripeR
import com.stripe.android.core.R as CoreR

internal fun ComposeTestRule.cardNumberField() = onNodeWithTextAfterWaiting(
    resourceString(StripeR.string.stripe_acc_label_card_number)
)

internal fun ComposeTestRule.cardExpirationField(): SemanticsNodeInteraction {
    waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
        onAllNodes(
            hasContentDescription("Expiration date", true)
        ).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    return onNodeWithContentDescription(label = "Expiration date", substring = true)
}

internal fun ComposeTestRule.cardCvcField() = onNodeWithTextAfterWaiting(
    resourceString(StripeR.string.stripe_cvc_number_hint)
)

internal fun ComposeTestRule.zipCodeField() = onNodeWithText(
    resourceString(CoreR.string.stripe_address_label_zip_code)
)

internal fun ComposeTestRule.onNodeWithTextAfterWaiting(text: String): SemanticsNodeInteraction {
    waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
        onAllNodes(hasText(text)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    return onNodeWithText(text)
}

private fun resourceString(id: Int): String {
    return InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)
}

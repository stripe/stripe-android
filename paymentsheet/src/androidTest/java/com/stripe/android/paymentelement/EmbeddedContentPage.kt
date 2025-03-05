package com.stripe.android.paymentelement

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT

internal class EmbeddedContentPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickOnLpm(code: String) {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"))
            .performScrollTo()
            .performClick()
    }
}

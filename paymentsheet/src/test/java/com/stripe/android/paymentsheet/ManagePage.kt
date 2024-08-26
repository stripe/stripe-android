package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON

internal class ManagePage(
    private val composeTestRule: ComposeTestRule
) {
    fun waitUntilVisible() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun selectPaymentMethod(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
        ).performScrollTo().performClick()
    }
}

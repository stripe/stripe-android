package com.stripe.android.tta.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG

internal fun ComposeTestRule.waitUntilLayoutWithPrimaryButtonMissing() {
    waitUntil(DEFAULT_UI_TIMEOUT) {
        onAllNodesWithTag(PRIMARY_BUTTON_TEST_TAG)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isEmpty()
    }

    waitUntil(DEFAULT_UI_TIMEOUT) {
        onAllNodesWithTag(TAP_TO_ADD_LAYOUT_TEST_TAG)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isEmpty()
    }
}

package com.stripe.android.tta.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_LAYOUT_TEST_TAG
import com.stripe.android.testing.waitUntilWithIdle

internal fun ComposeTestRule.waitUntilLayoutWithPrimaryButtonMissing(
    buttonTestTag: String,
) {
    waitUntilWithIdle {
        onAllNodesWithTag(buttonTestTag)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isEmpty()
    }

    waitUntilWithIdle {
        onAllNodesWithTag(TAP_TO_ADD_LAYOUT_TEST_TAG)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isEmpty()
    }
}

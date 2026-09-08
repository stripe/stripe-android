package com.stripe.android.tta.testing

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_LAYOUT_TEST_TAG
import com.stripe.android.testing.waitForNoNodes

internal fun ComposeTestRule.waitUntilLayoutWithPrimaryButtonMissing(
    buttonTestTag: String,
) {
    waitForNoNodes(
        matcher = hasTestTag(buttonTestTag),
        atLeastOneRootRequired = false,
    )

    waitForNoNodes(
        matcher = hasTestTag(TAP_TO_ADD_LAYOUT_TEST_TAG),
        atLeastOneRootRequired = false,
    )
}

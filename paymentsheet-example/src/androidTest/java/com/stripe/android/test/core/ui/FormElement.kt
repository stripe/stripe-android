package com.stripe.android.test.core.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT

class FormElement(
    private val composeTestRule: ComposeTestRule
) {
    @OptIn(ExperimentalTestApi::class)
    fun waitFor() {
        composeTestRule.waitUntilExactlyOneExists(
            hasTestTag(FORM_ELEMENT_TEST_TAG),
            DEFAULT_UI_TIMEOUT.inWholeMilliseconds
        )
    }
}

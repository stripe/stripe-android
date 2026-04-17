package com.stripe.android.tta.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_SHOWN_SCREEN_DELAY

class TapToAddDelayPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun advancePastScreen() {
        composeTestRule.mainClock.advanceTimeBy(TAP_TO_ADD_SHOWN_SCREEN_DELAY)
    }
}

package com.stripe.android.tta.testing

import androidx.compose.ui.test.junit4.ComposeTestRule

class TapToAddDelayPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun advancePastScreen() {
        composeTestRule.mainClock.advanceTimeBy(CARD_ADDED_SHOWN_DELAY)
    }

    private companion object {
        const val CARD_ADDED_SHOWN_DELAY = 2500L
    }
}

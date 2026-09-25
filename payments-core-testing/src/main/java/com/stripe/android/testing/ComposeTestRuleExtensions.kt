package com.stripe.android.testing

import androidx.compose.ui.test.junit4.ComposeTestRule

private const val WAIT_TIMEOUT_MILLIS = 5_000L

fun ComposeTestRule.waitUntilWithIdle(condition: () -> Boolean) {
    waitForIdle()

    waitUntil(WAIT_TIMEOUT_MILLIS) {
        condition()
    }
}

fun ComposeTestRule.waitUntilWithIdle(
    conditionDescription: String,
    condition: () -> Boolean,
) {
    waitForIdle()

    waitUntil(conditionDescription, WAIT_TIMEOUT_MILLIS) {
        condition()
    }
}

package com.stripe.android.checkout.webauth

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.time.Duration.Companion.seconds

internal class ThreeDS2ChallengePage(
    private val device: UiDevice,
) {
    fun complete() {
        val completeButton = device.wait(
            Until.findObject(By.text("COMPLETE")),
            CHALLENGE_TIMEOUT.inWholeMilliseconds,
        )
        checkNotNull(completeButton) { "3DS2 challenge COMPLETE button not found" }.click()
    }

    private companion object {
        val CHALLENGE_TIMEOUT = 30.seconds
    }
}

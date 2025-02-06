package com.stripe.android.test.core.ui

import androidx.test.uiautomator.UiDevice

class AddPaymentMethodButton(private val device: UiDevice) {
    fun isDisplayed(): Boolean {
        return UiAutomatorText("+ Add", className = null, device = device).exists()
    }
}

package com.stripe.android.test.core.ui

import android.widget.EditText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2

internal class LinkWebViewPage(device: UiDevice) {
    val emailButton = UiAutomatorElement(
        bySelector = By.hasDescendant(By.text("Email"))
            .hasDescendant(By.clazz(EditText::class.java)),
        device = device,
    ) { it.firstEditText() }
    val verificationInput = UiAutomatorElement(
        bySelector = By.hasDescendant(By.text("Enter your verification code"))
            .hasDescendant(By.clazz(EditText::class.java)),
        device = device,
    ) { it.firstEditText() }
    val payButton = UiAutomatorElement(
        bySelector = By.textContains("Pay $50.99").clickable(true),
        device = device,
    )
}

private fun UiObject2.firstEditText(): UiObject2? {
    if (className == EditText::class.java.name) {
        return this
    }
    for (child in children) {
        val result = child.firstEditText()
        if (result != null) {
            return result
        }
    }
    return null
}

package com.stripe.android.test.core.ui

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import java.security.InvalidParameterException

open class UiAutomatorText(
    private val label: String,
    var className: String? = "android.widget.TextView",
    private val device: UiDevice
) {
    private val selector: UiSelector
        get() = className?.let {
            UiSelector().textContains(label).className(it)
        } ?: UiSelector().textContains(label)

    open fun click() {
        if (!exists()) {
            if (!exists()) {
                scroll()
            }
        }
        if (!exists()) {
            throw InvalidParameterException("Text button not found: $label $className")
        }
        device.findObject(selector).click()
    }

    fun exists(): Boolean {
        val originalClassName = className
        className = if (device.findObject(selector).exists()) {
            originalClassName
        } else {
            null
        }
        return device.findObject(selector).exists()
    }

    fun wait(waitMs: Long) =
        device.wait(Until.findObject(By.text(label)), waitMs)

    fun scroll() {
        UiScrollable(UiSelector().scrollable(true))
            .scrollIntoView(selector)
    }
}

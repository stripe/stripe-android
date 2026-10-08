package com.stripe.android.testing

import android.widget.EditText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Condition
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import java.util.regex.Pattern

/** Condition-based interaction with both Compose accessibility nodes and browser/WebView content. */
class ExternalUiTestDriver {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun await(selector: BySelector): UiObject2 = await(selector, UI_TIMEOUT_MS)

    fun await(selector: BySelector, timeoutMillis: Long): UiObject2 {
        val condition = object : Condition<UiDevice, UiObject2> {
            override fun apply(device: UiDevice): UiObject2? {
                dismissChromeWelcome()
                return device.findObject(selector)?.takeUnless { it.visibleBounds.isEmpty }
            }
        }
        return checkNotNull(device.wait(condition, timeoutMillis)) {
            "Timed out waiting for $selector in ${device.currentPackageName}"
        }
    }

    fun click(selector: BySelector) {
        await(By.copy(selector).enabled(true)).click()
    }

    fun scrollTo(selector: BySelector): UiObject2 {
        val deadline = System.nanoTime() + UI_TIMEOUT_MS * NANOS_PER_MILLISECOND
        var direction = Direction.DOWN
        while (System.nanoTime() < deadline) {
            dismissChromeWelcome()
            device.findObject(selector)?.takeUnless { it.visibleBounds.isEmpty }?.let { return it }
            val scrollable = device.findObject(By.scrollable(true))
            if (scrollable != null) {
                if (!scrollable.scroll(direction, SCROLL_FRACTION)) {
                    direction = if (direction == Direction.DOWN) Direction.UP else Direction.DOWN
                }
            } else {
                // Browser pages may not expose a scrollable accessibility node.
                val upper = device.displayHeight / SCREEN_QUARTERS
                val lower = device.displayHeight - upper
                device.swipe(
                    device.displayWidth / 2,
                    if (direction == Direction.DOWN) lower else upper,
                    device.displayWidth / 2,
                    if (direction == Direction.DOWN) upper else lower,
                    SWIPE_STEPS,
                )
                device.waitForIdle(SCROLL_IDLE_TIMEOUT_MS)
            }
        }
        error("Timed out scrolling to $selector in ${device.currentPackageName}")
    }

    fun enterText(selector: BySelector, value: String) {
        scrollTo(selector).click()
        // Placeholders and labels can be separate nodes from the editable Compose field.
        val field = await(By.clazz(EditText::class.java).focused(true))
        field.text = value
        hideKeyboard()
    }

    fun hideKeyboard() {
        if (device.hasObject(By.pkg(Pattern.compile(".*inputmethod.*")))) {
            device.pressBack()
        }
    }

    private fun dismissChromeWelcome() {
        listOf("signin_fre_dismiss_button", "terms_accept", "negative_button").forEach { name ->
            device.findObject(By.res("com.android.chrome", name))?.click()
        }
    }

    companion object {
        private const val UI_TIMEOUT_MS = 60_000L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
        private const val SCROLL_FRACTION = 0.7f
        private const val SCREEN_QUARTERS = 4
        private const val SWIPE_STEPS = 20
        private const val SCROLL_IDLE_TIMEOUT_MS = 500L

        fun id(value: String): BySelector = By.res(
            Pattern.compile("(?:.*:id/)?${Pattern.quote(value)}")
        )

        fun text(value: String): BySelector = By.text(value)

        fun textMatching(value: String): BySelector = By.text(Pattern.compile(value, Pattern.DOTALL))
    }
}

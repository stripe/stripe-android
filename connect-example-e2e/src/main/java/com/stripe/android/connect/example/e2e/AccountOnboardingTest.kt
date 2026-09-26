package com.stripe.android.connect.example.e2e

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
internal class AccountOnboardingTest {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.pressHome()
    }

    @Test
    fun accountOnboardingLoads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launchIntent = checkNotNull(context.packageManager.getLaunchIntentForPackage(CONNECT_APP)) {
            "No launch intent for $CONNECT_APP"
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(launchIntent)

        clickResource("settings_button", timeoutMs = 60_000)
        swipeUp(2)
        replaceResourceText("other_account_input", "acct_1RKLk9PwPtoT2bUJ", timeoutMs = 60_000)
        device.pressBack()
        clickResource("save_button")
        clickTextContainer("Account Onboarding", timeoutMs = 60_000)
        clickTextRegex(".*Add information", timeoutMs = 120_000)
        dismissChromeFirstRunIfPresent()
        waitForOnboardingEntry(timeoutMs = 60_000)
    }

    private fun clickResource(id: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        findResource(id, timeoutMs).click()
        device.waitForIdle()
    }

    private fun replaceResourceText(id: String, value: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        findResource(id, timeoutMs).apply {
            click()
            text = value
        }
    }

    private fun findResource(id: String, timeoutMs: Long): UiObject2 {
        return device.wait(Until.findObject(resourceSelector(id)), timeoutMs)
            ?: failWithUi("resource id", id)
    }

    private fun dismissChromeFirstRunIfPresent() {
        repeat(3) {
            val control = findOptionalResource(
                ids = arrayOf("signin_fre_dismiss_button", "terms_accept", "negative_button"),
                timeoutMs = CHROME_PROMPT_TIMEOUT_MS,
            ) ?: return
            control.click()
            device.waitForIdle()
        }
    }

    private fun findOptionalResource(ids: Array<String>, timeoutMs: Long): UiObject2? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            for (id in ids) {
                device.findObject(resourceSelector(id))?.let { return it }
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    private fun resourceSelector(id: String): BySelector {
        return By.res(Pattern.compile(".*${Pattern.quote(id)}"))
    }

    private fun waitForText(text: String, timeoutMs: Long) {
        device.wait(Until.findObject(By.text(text)), timeoutMs)
            ?: failWithUi("text", text)
    }

    private fun waitForOnboardingEntry(timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (device.findObject(By.text("Review and confirm")) != null) {
                return
            }
            if (device.findObject(By.text("Welcome back")) != null) {
                val remainingMs = (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
                waitForTextRegex(".*Send code", remainingMs)
                return
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        failWithUi("onboarding entry text", "Review and confirm or Welcome back")
    }

    private fun clickTextContainer(text: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var node = device.findObject(By.text(text))
            try {
                while (node != null && !node.isClickable) {
                    node = node.parent
                }
                if (node != null) {
                    node.click()
                    device.waitForIdle()
                    return
                }
            } catch (_: StaleObjectException) {
                // The Compose hierarchy changed; reacquire the row until the deadline.
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        failWithUi("clickable ancestor for text", text)
    }

    private fun clickTextRegex(regex: String, timeoutMs: Long) {
        val node = waitForTextRegex(regex, timeoutMs)
        node.click()
        device.waitForIdle()
    }

    private fun waitForTextRegex(regex: String, timeoutMs: Long): UiObject2 {
        return device.wait(Until.findObject(By.text(Pattern.compile(regex))), timeoutMs)
            ?: failWithUi("text regex", regex)
    }

    private fun swipeUp(count: Int) {
        repeat(count) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 4 / 5,
                device.displayWidth / 2,
                device.displayHeight / 4,
                20,
            )
            device.waitForIdle()
        }
    }

    private fun failWithUi(selectorType: String, selector: String): Nothing {
        val hierarchy = runCatching {
            ByteArrayOutputStream().use { output ->
                device.dumpWindowHierarchy(output)
                output.toString(Charsets.UTF_8.name()).take(MAX_HIERARCHY_LENGTH)
            }
        }.getOrElse { error ->
            "Unavailable: ${error.message}"
        }
        fail(
            "Could not find $selectorType '$selector'. " +
                "Current package: ${device.currentPackageName}. Hierarchy: $hierarchy",
        )
        error("unreachable")
    }

    private companion object {
        const val CONNECT_APP = "com.stripe.android.connect.example"
        const val CHROME_PROMPT_TIMEOUT_MS = 5_000L
        const val DEFAULT_TIMEOUT_MS = 30_000L
        const val MAX_HIERARCHY_LENGTH = 8_000
        const val POLL_INTERVAL_MS = 250L
    }
}

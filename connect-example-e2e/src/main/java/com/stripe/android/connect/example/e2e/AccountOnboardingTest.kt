package com.stripe.android.connect.example.e2e

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
        clickResource("AccountOnboarding", timeoutMs = 60_000)
        waitForText("Review and confirm", timeoutMs = 60_000)
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
        val selector = By.res(Pattern.compile(".*${Pattern.quote(id)}"))
        return device.wait(Until.findObject(selector), timeoutMs)
            ?: failWithUi("resource id", id)
    }

    private fun waitForText(text: String, timeoutMs: Long) {
        device.wait(Until.findObject(By.text(text)), timeoutMs)
            ?: failWithUi("text", text)
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
        fail("Could not find $selectorType '$selector'. Current package: ${device.currentPackageName}")
        error("unreachable")
    }

    private companion object {
        const val CONNECT_APP = "com.stripe.android.connect.example"
        const val DEFAULT_TIMEOUT_MS = 30_000L
    }
}

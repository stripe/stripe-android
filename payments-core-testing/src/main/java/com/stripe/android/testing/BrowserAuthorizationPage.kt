package com.stripe.android.testing

import android.os.SystemClock.sleep
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/** Waits for Stripe test authorization pages in Chrome and dismisses Chrome's first-run onboarding. */
class BrowserAuthorizationPage(
    private val device: UiDevice,
    private val composeTestRule: ComposeTestRule,
) {
    fun awaitChromeLaunch() {
        composeTestRule.waitUntil(
            conditionDescription = "Chrome to launch",
            timeoutMillis = BROWSER_LAUNCH_TIMEOUT_MS,
        ) {
            currentTopActivity()?.contains(CHROME_PACKAGE) == true
        }
        dismissFirstRunIfShown()
    }

    fun awaitPageText(text: String, timeoutMs: Long) {
        checkNotNull(
            device.wait(
                Until.findObject(By.textContains(text)),
                timeoutMs,
            )
        ) {
            "Authorization page content '$text' did not load"
        }
        device.waitForIdle()
    }

    fun clickAction(label: String) {
        checkNotNull(device.findObject(By.textContains(label))) {
            "Authorization page action '$label' not found"
        }.click()
    }

    fun currentTopActivity(): String? {
        return runCatching {
            device.executeShellCommand("dumpsys activity activities")
                .lineSequence()
                .firstOrNull { "topResumedActivity=" in it }
        }.getOrNull()
    }

    fun dismissFirstRunIfShown() {
        sleep(CHROME_ACTIVITY_TRANSITION_MS)
        if (currentTopActivity()?.contains(CHROME_FIRST_RUN_ACTIVITY) != true) {
            return
        }

        repeat(CHROME_FIRST_RUN_MAX_SCREENS) {
            if (currentTopActivity()?.contains(CHROME_FIRST_RUN_ACTIVITY) == false) {
                return
            }

            var buttonId: String? = null
            composeTestRule.waitUntil(
                conditionDescription = "Chrome first-run dismissal button to appear",
                timeoutMillis = BROWSER_LAUNCH_TIMEOUT_MS,
            ) {
                buttonId = CHROME_FIRST_RUN_BUTTON_IDS.firstOrNull { id ->
                    device.hasObject(By.res("$CHROME_PACKAGE:id/$id"))
                }
                buttonId != null
            }
            device.findObject(By.res("$CHROME_PACKAGE:id/${checkNotNull(buttonId)}")).click()
            sleep(CHROME_ACTIVITY_TRANSITION_MS)
        }

        composeTestRule.waitUntil(
            conditionDescription = "Chrome first-run onboarding to close",
            timeoutMillis = BROWSER_LAUNCH_TIMEOUT_MS,
        ) {
            currentTopActivity()?.contains(CHROME_FIRST_RUN_ACTIVITY) == false
        }
    }

    companion object {
        const val BROWSER_LAUNCH_TIMEOUT_MS = 60_000L

        private const val CHROME_PACKAGE = "com.android.chrome"
        private const val CHROME_ACTIVITY_TRANSITION_MS = 2_000L
        private const val CHROME_FIRST_RUN_MAX_SCREENS = 8
        private const val CHROME_FIRST_RUN_ACTIVITY =
            "com.android.chrome/org.chromium.chrome.browser.firstrun.FirstRunActivity"
        private val CHROME_FIRST_RUN_BUTTON_IDS = listOf(
            "signin_fre_dismiss_button",
            "negative_button",
            "button_secondary",
            "terms_accept",
            "next_button",
        )
    }
}

package com.stripe.android.connect

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
internal class StripeConnectRedirectActivityTest {

    @get:Rule
    val intentsTestRule = IntentsRule()

    private val packageName = ApplicationProvider.getApplicationContext<Application>().packageName
    private val deepLinkUri = "stripe-connect://$packageName"
    private val customTabUrl = "https://localhost:443/"

    private val redirectIntentMatcher
        get() = hasComponent(StripeConnectRedirectActivity::class.java.name)

    private fun chromeIntentMatcher(url: String? = null): Matcher<Intent> {
        val matchers = listOfNotNull(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.toPackage("com.android.chrome"),
            url?.let { IntentMatchers.hasData(Uri.parse(it)) }
        )
        return CoreMatchers.allOf(matchers)
    }

    @Test
    fun testDefaultOpensAndFinishesImmediately() {
        launch(viewIntent(deepLinkUri)).use {
            assertThat(it.state).isEqualTo(Lifecycle.State.DESTROYED)
            intended(redirectIntentMatcher)
        }
    }

    @Test
    fun testIntentWithUrlOpensCustomTab() {
        launchWithCustomTabUrl(customTabUrl).use {
            assertThat(it.state).isEqualTo(Lifecycle.State.CREATED)
            intended(redirectIntentMatcher)
            intended(chromeIntentMatcher(customTabUrl))
        }
    }

    @Test
    fun testNewIntentFinishes() {
        launchWithCustomTabUrl(customTabUrl).use { scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.CREATED)
            launch(viewIntent(deepLinkUri)).use { innerScenario ->
                assertThat(innerScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            }
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testClosingCustomTabClosesActivity() = runTest {
        launchWithCustomTabUrl(customTabUrl).use {
            assertThat(it.state).isEqualTo(Lifecycle.State.CREATED)
            closeCustomTab()
            pollUntil(5.seconds) { it.state == Lifecycle.State.DESTROYED }
        }
    }

    private fun viewIntent(uri: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    }

    private fun launch(intent: Intent): ActivityScenario<StripeConnectRedirectActivity> {
        return ActivityScenario.launch(intent)
    }

    private fun launchWithCustomTabUrl(url: String): ActivityScenario<StripeConnectRedirectActivity> {
        val intent =
            StripeConnectRedirectActivity.customTabIntent(
                context = ApplicationProvider.getApplicationContext(),
                url = url
            )
        return ActivityScenario.launch(intent)
    }

    private fun closeCustomTab() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.findObject(UiSelector().packageName("com.android.chrome")).waitForExists(5_000L)
        device.pressBack()
    }

    private suspend inline fun pollUntil(timeout: Duration, crossinline condition: () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(timeout) {
                while (!condition()) {
                    // Poll.
                    delay(50L)
                }
            }
        }
    }
}

package com.stripe.android.paymentsheet

import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
internal class PaymentSheetTest {
    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<MainActivity> = activityScenarioRule()

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun testPaymentSheet() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.addHeader("request-id", "mock_elements_sessions_request_id")
            response.testBodyFromFile("elements-sessions.json")
        }

        val countDownLatch = CountDownLatch(1)
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(it) { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Canceled::class.java)
                countDownLatch.countDown()
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_123_secret_123",
                configuration = null,
            )
        }
        waitForElementWithText("Pay")
        pressBack()
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    private fun waitForElementWithText(text: String) {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val selector = UiSelector().text(text)
        if (!uiDevice.findObject(selector).waitForExists(10_000)) {
            // This will fail and nicely show the view hierarchy.
            onView(withText(text)).check(matches(isDisplayed()))
        }
    }
}

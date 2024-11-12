package com.stripe.example.activity

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.stripe.example.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class PaymentSessionActivityTest {

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<LauncherActivity> = activityScenarioRule()

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
        BackgroundTaskTracker.reset()
    }

    @Test
    fun testSelectPaymentMethod() {
        val counter = CountDownLatch(1)

        BackgroundTaskTracker.onStop = {
            counter.countDown()
        }

        // launch PaymentSessionActivity
        Espresso.onView(ViewMatchers.withId(R.id.examples)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(5, click())
        )

        // on PaymentSessionActivity
        Espresso.onView(ViewMatchers.withId(R.id.select_payment_method_button))
            .perform(click())

        // navigate to PaymentMethodsActivity
        counter.await(5000L, TimeUnit.MILLISECONDS)
    }
}

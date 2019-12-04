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

@RunWith(AndroidJUnit4::class)
@LargeTest
class PaymentSessionActivityTest {

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<LauncherActivity> = activityScenarioRule()

    @get:Rule
    val idlingResourceRule: IdlingResourceRule = IdlingResourceRule("PaymentSessionActivityTest")

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun test() {
        // launch PaymentSessionActivity
        Espresso.onView(ViewMatchers.withId(R.id.examples)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(5, click())
        )

        // on PaymentSessionActivity
        Espresso.onView(ViewMatchers.withId(R.id.btn_select_payment_method))
            .perform(click())

        // on PaymentMethodsActivity
        Espresso.onView(ViewMatchers.withId(R.id.payment_methods_recycler)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
    }
}

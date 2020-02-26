package com.stripe.example.activity

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.stripe.android.CustomerSession
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import com.stripe.example.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddFpxPaymentMethodTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<LauncherActivity> = activityScenarioRule()

    @Before
    fun setup() {
        CustomerSession.initCustomerSession(context, object : EphemeralKeyProvider {
            override fun createEphemeralKey(
                apiVersion: String,
                keyUpdateListener: EphemeralKeyUpdateListener
            ) {
            }
        })
    }

    @Test
    fun launchFpxAndSelectBank() {
        // launch FPX selection activity
        onView(withId(R.id.examples)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(9, click())
        )

        // click select payment method button
        onView(withId(R.id.select_payment_method_button))
            .perform(click())

        // click on first bank in the list
        onView(withId(R.id.fpx_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
    }
}

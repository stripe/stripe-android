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
import com.stripe.android.R as StripeR

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddNetbankingPaymentMethodTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<LauncherActivity> = activityScenarioRule()

    @Before
    fun setup() {
        CustomerSession.initCustomerSession(
            context,
            object : EphemeralKeyProvider {
                override fun createEphemeralKey(
                    apiVersion: String,
                    keyUpdateListener: EphemeralKeyUpdateListener
                ) {
                }
            }
        )
    }

    @Test
    fun launchNetbankingAndSelectBank() {
        launchBankSelector()

        // click on first bank in the list
        onView(withId(StripeR.id.bank_list)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
    }

    @Test
    fun launchNetbankingAndConfirmWithoutSelectingBank() {
        launchBankSelector()

        // confirm selection without selecting a bank
        onView(withId(StripeR.id.action_save)).perform(click())

        // Nothing should happen as no bank was selected
    }

    private fun launchBankSelector() {
        // launch Netbanking selection activity
        onView(withId(R.id.examples)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(13, click())
        )

        // click select payment method button
        onView(withId(R.id.select_payment_method_button))
            .perform(click())
    }
}

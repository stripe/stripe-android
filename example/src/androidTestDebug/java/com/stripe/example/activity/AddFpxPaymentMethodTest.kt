package com.stripe.example.activity

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.stripe.android.CustomerSession
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.AddPaymentMethodActivityStarter
import com.stripe.example.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddFpxPaymentMethodTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }

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
        ActivityScenario.launch<AddPaymentMethodActivity>(
            Intent(context, AddPaymentMethodActivity::class.java)
                .putExtra(
                    "extra_activity_args",
                    AddPaymentMethodActivityStarter.Args.Builder()
                        .setPaymentMethodType(PaymentMethod.Type.Fpx)
                        .build()
                )
        ).use {
            it.onActivity {
                onView(withId(R.id.fpx_list)).perform(
                    RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, ViewActions.click())
                )
            }
        }
    }
}

package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class PaymentSheetTestRunnerContext(
    private val scenario: ActivityScenario<MainActivity>,
    private val paymentSheet: PaymentSheet,
    private val countDownLatch: CountDownLatch,
) {

    fun presentPaymentSheet(
        block: PaymentSheet.() -> Unit,
    ) {
        var unregisterer: (() -> Unit) = { }
        val paymentSheetLaunchedCountDownLatch = CountDownLatch(1)

        scenario.onActivity {
            val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                }

                override fun onActivityStarted(activity: Activity) {
                }

                override fun onActivityResumed(activity: Activity) {
                    if (activity is PaymentSheetActivity) {
                        paymentSheetLaunchedCountDownLatch.countDown()
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                }

                override fun onActivityStopped(activity: Activity) {
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                }

                override fun onActivityDestroyed(activity: Activity) {
                }

            }
            val application = it.applicationContext as Application
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            unregisterer = {
                application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
            }
            paymentSheet.block()
        }
        try {
            if (!paymentSheetLaunchedCountDownLatch.await(5, TimeUnit.SECONDS)) {
                println("PaymentSheet failed to launch.")
            }
        } finally {
            unregisterer()
        }
    }

    /**
     * Normally we know a test succeeds when it calls [PaymentSheetResultCallback], but some tests
     * succeed based on other criteria. In these cases, call this method to manually mark a test as
     * succeeded.
     */
    fun markTestSucceeded() {
        countDownLatch.countDown()
    }
}

internal fun ActivityScenarioRule<MainActivity>.runPaymentSheetTest(
    integrationType: IntegrationType,
    createIntentCallback: CreateIntentCallback? = null,
    resultCallback: PaymentSheetResultCallback,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    val factory = PaymentSheetTestFactory(
        integrationType = integrationType,
        createIntentCallback = createIntentCallback,
        resultCallback = { result ->
            resultCallback.onPaymentSheetResult(result)
            countDownLatch.countDown()
        }
    )

    runPaymentSheetTestInternal(
        countDownLatch = countDownLatch,
        makePaymentSheet = factory::make,
        block = block,
    )
}

private fun ActivityScenarioRule<MainActivity>.runPaymentSheetTestInternal(
    countDownLatch: CountDownLatch,
    makePaymentSheet: (ComponentActivity) -> PaymentSheet,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    scenario.moveToState(Lifecycle.State.CREATED)
    scenario.onActivity {
        PaymentConfiguration.init(it, "pk_test_123")
        LinkStore(it.applicationContext).clear()
    }

    lateinit var paymentSheet: PaymentSheet
    scenario.onActivity {
        paymentSheet = makePaymentSheet(it)
    }

    scenario.moveToState(Lifecycle.State.RESUMED)

    val testContext = PaymentSheetTestRunnerContext(scenario, paymentSheet, countDownLatch)
    block(testContext)

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
}

package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.networktesting.NetworkRule
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
        val activityLaunchObserver = ActivityLaunchObserver(PaymentSheetActivity::class.java)
        scenario.onActivity {
            activityLaunchObserver.prepareForLaunch(it)
            paymentSheet.block()
        }
        activityLaunchObserver.awaitLaunch()
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

internal fun runPaymentSheetTest(
    networkRule: NetworkRule,
    integrationType: IntegrationType = IntegrationType.Compose,
    createIntentCallback: CreateIntentCallback? = null,
    successTimeoutSeconds: Long = 5L,
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
        networkRule = networkRule,
        countDownLatch = countDownLatch,
        countDownLatchTimeoutSeconds = successTimeoutSeconds,
        makePaymentSheet = factory::make,
        block = block,
    )
}

private fun runPaymentSheetTestInternal(
    networkRule: NetworkRule,
    countDownLatch: CountDownLatch,
    countDownLatchTimeoutSeconds: Long,
    makePaymentSheet: (ComponentActivity) -> PaymentSheet,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
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

        val didCompleteSuccessfully = countDownLatch.await(countDownLatchTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
    }
}

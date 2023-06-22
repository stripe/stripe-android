package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentSheet
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
        scenario.onActivity {
            paymentSheet.block()
        }
    }

    fun markTestSucceeded() {
        countDownLatch.countDown()
    }
}

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal fun runPaymentSheetTest(
    createIntentCallback: CreateIntentCallback? = null,
    resultCallback: PaymentSheetResultCallback,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    for (integrationType in PaymentSheetTestFactory.IntegrationType.values()) {
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
}

private fun runPaymentSheetTestInternal(
    countDownLatch: CountDownLatch,
    makePaymentSheet: (ComponentActivity) -> PaymentSheet,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.moveToState(Lifecycle.State.CREATED)

    scenario.onActivity {
        PaymentConfiguration.init(it, "pk_test_123")
    }

    lateinit var paymentSheet: PaymentSheet
    scenario.onActivity {
        paymentSheet = makePaymentSheet(it)
    }

    scenario.moveToState(Lifecycle.State.RESUMED)

    val testContext = PaymentSheetTestRunnerContext(scenario, paymentSheet, countDownLatch)
    block(testContext)

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    scenario.close()
}

package com.stripe.android.paymentsheet.utils

import androidx.activity.compose.setContent
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
    runPaymentSheetTestInternal(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallbacks = listOf(createIntentCallback),
        countDownLatchTimeoutSeconds = successTimeoutSeconds,
        resultCallbacks = listOf(resultCallback),
        block = block,
    )
}

internal fun runMultiplePaymentSheetInstancesTest(
    networkRule: NetworkRule,
    firstCreateIntentCallback: CreateIntentCallback? = null,
    secondCreateIntentCallback: CreateIntentCallback? = null,
    successTimeoutSeconds: Long = 5L,
    firstResultCallback: PaymentSheetResultCallback,
    secondResultCallback: PaymentSheetResultCallback,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    runPaymentSheetTestInternal(
        networkRule = networkRule,
        integrationType = IntegrationType.Compose,
        createIntentCallbacks = listOf(firstCreateIntentCallback, secondCreateIntentCallback),
        countDownLatchTimeoutSeconds = successTimeoutSeconds,
        resultCallbacks = listOf(firstResultCallback, secondResultCallback),
        block = block,
    )
}

private fun runPaymentSheetTestInternal(
    networkRule: NetworkRule,
    integrationType: IntegrationType,
    createIntentCallbacks: List<CreateIntentCallback?>,
    countDownLatchTimeoutSeconds: Long,
    resultCallbacks: List<PaymentSheetResultCallback>,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            LinkStore(it.applicationContext).clear()
        }

        val paymentSheets = mutableMapOf<PaymentSheet, CountDownLatch>()
        scenario.onActivity { activity ->
            when (integrationType) {
                IntegrationType.Compose -> {
                    activity.setContent {
                        resultCallbacks.forEachIndexed { index, resultCallback ->
                            val countDownLatch = CountDownLatch(1)

                            val factory = PaymentSheetTestFactory(
                                createIntentCallback = createIntentCallbacks.getOrNull(index),
                                resultCallback = { result ->
                                    resultCallback.onPaymentSheetResult(result)
                                    countDownLatch.countDown()
                                }
                            )

                            paymentSheets[factory.make()] = countDownLatch
                        }
                    }
                }
                IntegrationType.Activity -> {
                    resultCallbacks.forEachIndexed { index, resultCallback ->
                        val countDownLatch = CountDownLatch(1)

                        val factory = PaymentSheetTestFactory(
                            createIntentCallback = createIntentCallbacks.getOrNull(index),
                            resultCallback = { result ->
                                resultCallback.onPaymentSheetResult(result)
                                countDownLatch.countDown()
                            }
                        )

                        paymentSheets[factory.make(activity)] = countDownLatch
                    }
                }
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        paymentSheets.forEach { (paymentSheet, countDownLatch) ->
            scenario.onActivity {
                LinkStore(it.applicationContext).clear()
            }

            val testContext = PaymentSheetTestRunnerContext(scenario, paymentSheet, countDownLatch)
            block(testContext)

            val didCompleteSuccessfully = countDownLatch.await(countDownLatchTimeoutSeconds, TimeUnit.SECONDS)
            networkRule.validate()
            assertThat(didCompleteSuccessfully).isTrue()
        }
    }
}

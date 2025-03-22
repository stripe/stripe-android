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
    val countDownLatch = CountDownLatch(1)

    val paymentSheetBuilder = PaymentSheet.Builder { result ->
        resultCallback.onPaymentSheetResult(result)
        countDownLatch.countDown()
    }.apply {
        createIntentCallback?.let { createIntentCallback(it) }
    }

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            LinkStore(it.applicationContext).clear()
        }

        lateinit var paymentSheet: PaymentSheet

        scenario.onActivity { activity ->
            when (integrationType) {
                IntegrationType.Compose -> activity.setContent {
                    paymentSheet = paymentSheetBuilder.build()
                }
                IntegrationType.Activity -> {
                    paymentSheet = paymentSheetBuilder.build(activity)
                }
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val testContext = PaymentSheetTestRunnerContext(scenario, paymentSheet, countDownLatch)
        block(testContext)

        val didCompleteSuccessfully = countDownLatch.await(successTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
    }
}

internal fun runMultiplePaymentSheetInstancesTest(
    networkRule: NetworkRule,
    testType: MultipleInstancesTestType,
    createIntentCallback: CreateIntentCallback,
    successTimeoutSeconds: Long = 5L,
    resultCallback: PaymentSheetResultCallback,
    block: (PaymentSheetTestRunnerContext) -> Unit,
) {
    var firstCreateIntentCallbackCalled = false
    var secondCreateIntentCallbackCalled = false

    val countDownLatch = CountDownLatch(1)

    val firstPaymentSheetBuilder = PaymentSheet.Builder { result ->
        if (testType == MultipleInstancesTestType.RunWithFirst) {
            resultCallback.onPaymentSheetResult(result)
            countDownLatch.countDown()
        } else {
            error("Should not have been called!")
        }
    }.createIntentCallback { paymentMethod, shouldSavePaymentMethod ->
        if (testType == MultipleInstancesTestType.RunWithFirst) {
            firstCreateIntentCallbackCalled = true

            createIntentCallback.onCreateIntent(paymentMethod, shouldSavePaymentMethod)
        } else {
            error("Should not have been called!")
        }
    }

    val secondPaymentSheetBuilder = PaymentSheet.Builder { result ->
        if (testType == MultipleInstancesTestType.RunWithSecond) {
            resultCallback.onPaymentSheetResult(result)
            countDownLatch.countDown()
        } else {
            error("Should not have been called!")
        }
    }.createIntentCallback { paymentMethod, shouldSavePaymentMethod ->
        if (testType == MultipleInstancesTestType.RunWithSecond) {
            secondCreateIntentCallbackCalled = true

            createIntentCallback.onCreateIntent(paymentMethod, shouldSavePaymentMethod)
        } else {
            error("Should not have been called!")
        }
    }

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            LinkStore(it.applicationContext).clear()
        }

        lateinit var firstPaymentSheet: PaymentSheet
        lateinit var secondPaymentSheet: PaymentSheet

        scenario.onActivity { activity ->
            activity.setContent {
                firstPaymentSheet = firstPaymentSheetBuilder.build()
                secondPaymentSheet = secondPaymentSheetBuilder.build()
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val paymentSheet = if (testType == MultipleInstancesTestType.RunWithFirst) {
            firstPaymentSheet
        } else {
            secondPaymentSheet
        }

        val testContext = PaymentSheetTestRunnerContext(scenario, paymentSheet, countDownLatch)
        block(testContext)

        val didCompleteSuccessfully = countDownLatch.await(successTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()

        if (testType == MultipleInstancesTestType.RunWithFirst) {
            assertThat(firstCreateIntentCallbackCalled).isTrue()
            assertThat(secondCreateIntentCallbackCalled).isFalse()
        } else {
            assertThat(firstCreateIntentCallbackCalled).isFalse()
            assertThat(secondCreateIntentCallbackCalled).isTrue()
        }
    }
}

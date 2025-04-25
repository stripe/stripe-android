package com.stripe.android.paymentsheet.utils

import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FlowControllerTestRunnerContext(
    private val scenario: ActivityScenario<MainActivity>,
    val flowController: PaymentSheet.FlowController,
    val configureCallbackTurbine: Turbine<PaymentOption?>,
    private val countDownLatch: CountDownLatch,
) {

    fun configureFlowController(
        block: PaymentSheet.FlowController.() -> Unit,
    ) {
        val activityLaunchObserver = ActivityLaunchObserver(PaymentOptionsActivity::class.java)
        scenario.onActivity {
            activityLaunchObserver.prepareForLaunch(it)
            flowController.block()
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

internal fun runFlowControllerTest(
    networkRule: NetworkRule,
    integrationType: IntegrationType = IntegrationType.Compose,
    callConfirmOnPaymentOptionCallback: Boolean = true,
    builder: PaymentSheet.FlowController.Builder.() -> Unit = {},
    resultCallback: PaymentSheetResultCallback,
    block: suspend (FlowControllerTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)
    val configureCallbackTurbine = Turbine<PaymentOption?>()

    val factory = FlowControllerTestFactory(
        callConfirmOnPaymentOptionCallback = callConfirmOnPaymentOptionCallback,
        builder = builder,
        configureCallbackTurbine = configureCallbackTurbine,
        resultCallback = { result ->
            resultCallback.onPaymentSheetResult(result)
            countDownLatch.countDown()
        }
    )

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)

        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            LinkStore(it.applicationContext).clear()
        }

        var flowController: PaymentSheet.FlowController? = null

        scenario.onActivity { activity ->
            when (integrationType) {
                IntegrationType.Compose -> activity.setContent {
                    flowController = factory.make()
                }
                IntegrationType.Activity -> {
                    flowController = factory.make(activity)
                }
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val testContext = FlowControllerTestRunnerContext(
            scenario = scenario,
            flowController = flowController ?: throw IllegalStateException(
                "FlowController should have been created!"
            ),
            configureCallbackTurbine = configureCallbackTurbine,
            countDownLatch = countDownLatch,
        )
        runTest {
            block(testContext)
        }

        testContext.configureCallbackTurbine.ensureAllEventsConsumed()

        val didCompleteSuccessfully = countDownLatch.await(5, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
    }
}

internal fun runMultipleFlowControllerInstancesTest(
    networkRule: NetworkRule,
    testType: MultipleInstancesTestType,
    callConfirmOnPaymentOptionCallback: Boolean = true,
    createIntentCallback: CreateIntentCallback,
    resultCallback: PaymentSheetResultCallback,
    block: suspend (FlowControllerTestRunnerContext) -> Unit,
) {
    var firstCreateIntentCallbackCalled = false
    var secondCreateIntentCallbackCalled = false

    val countDownLatch = CountDownLatch(1)
    val configureCallbackTurbine = Turbine<PaymentOption?>()

    val firstFlowControllerFactory = FlowControllerTestFactory(
        callConfirmOnPaymentOptionCallback = callConfirmOnPaymentOptionCallback,
        builder = {
            createIntentCallback { paymentMethod, shouldSavePaymentMethod ->
                if (testType == MultipleInstancesTestType.RunWithFirst) {
                    firstCreateIntentCallbackCalled = true

                    createIntentCallback.onCreateIntent(paymentMethod, shouldSavePaymentMethod)
                } else {
                    error("Should not have been called!")
                }
            }
        },
        configureCallbackTurbine = configureCallbackTurbine,
        resultCallback = { result ->
            if (testType == MultipleInstancesTestType.RunWithFirst) {
                resultCallback.onPaymentSheetResult(result)
                countDownLatch.countDown()
            } else {
                error("Should not have been called!")
            }
        }
    )

    val secondFlowControllerFactory = FlowControllerTestFactory(
        callConfirmOnPaymentOptionCallback = callConfirmOnPaymentOptionCallback,
        builder = {
            createIntentCallback { paymentMethod, shouldSavePaymentMethod ->
                if (testType == MultipleInstancesTestType.RunWithSecond) {
                    secondCreateIntentCallbackCalled = true

                    createIntentCallback.onCreateIntent(paymentMethod, shouldSavePaymentMethod)
                } else {
                    error("Should not have been called!")
                }
            }
        },
        configureCallbackTurbine = configureCallbackTurbine,
        resultCallback = { result ->
            if (testType == MultipleInstancesTestType.RunWithSecond) {
                resultCallback.onPaymentSheetResult(result)
                countDownLatch.countDown()
            } else {
                error("Should not have been called!")
            }
        }
    )

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            LinkStore(it.applicationContext).clear()
        }

        lateinit var firstFlowController: PaymentSheet.FlowController
        lateinit var secondFlowController: PaymentSheet.FlowController

        scenario.onActivity { activity ->
            activity.setContent {
                firstFlowController = firstFlowControllerFactory.make()
                secondFlowController = secondFlowControllerFactory.make()
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val flowController = if (testType == MultipleInstancesTestType.RunWithFirst) {
            firstFlowController
        } else {
            secondFlowController
        }

        val testContext = FlowControllerTestRunnerContext(
            scenario = scenario,
            flowController = flowController,
            configureCallbackTurbine = configureCallbackTurbine,
            countDownLatch = countDownLatch,
        )
        runTest {
            block(testContext)
        }

        testContext.configureCallbackTurbine.ensureAllEventsConsumed()

        val didCompleteSuccessfully = countDownLatch.await(5, TimeUnit.SECONDS)
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

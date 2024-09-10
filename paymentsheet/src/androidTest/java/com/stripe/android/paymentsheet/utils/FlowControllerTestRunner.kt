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
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FlowControllerTestRunnerContext(
    private val scenario: ActivityScenario<MainActivity>,
    val flowController: PaymentSheet.FlowController,
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
    integrationType: IntegrationType,
    callConfirmOnPaymentOptionCallback: Boolean = true,
    createIntentCallback: CreateIntentCallback? = null,
    paymentOptionCallback: PaymentOptionCallback,
    resultCallback: PaymentSheetResultCallback,
    block: (FlowControllerTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    val factory = FlowControllerTestFactory(
        callConfirmOnPaymentOptionCallback = callConfirmOnPaymentOptionCallback,
        integrationType = integrationType,
        createIntentCallback = createIntentCallback,
        paymentOptionCallback = paymentOptionCallback,
        resultCallback = { result ->
            resultCallback.onPaymentSheetResult(result)
            countDownLatch.countDown()
        }
    )

    runFlowControllerTest(
        networkRule = networkRule,
        countDownLatch = countDownLatch,
        makeFlowController = factory::make,
        block = block,
    )
}

private fun runFlowControllerTest(
    networkRule: NetworkRule,
    countDownLatch: CountDownLatch,
    makeFlowController: (ComponentActivity) -> PaymentSheet.FlowController,
    block: (FlowControllerTestRunnerContext) -> Unit,
) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)

        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            LinkStore(it.applicationContext).clear()
        }

        var flowController: PaymentSheet.FlowController? = null

        scenario.onActivity { activity ->
            flowController = makeFlowController(activity)
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val testContext = FlowControllerTestRunnerContext(
            scenario = scenario,
            flowController = flowController ?: throw IllegalStateException(
                "FlowController should have been created!"
            ),
            countDownLatch = countDownLatch,
        )
        block(testContext)

        val didCompleteSuccessfully = countDownLatch.await(5, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
    }
}

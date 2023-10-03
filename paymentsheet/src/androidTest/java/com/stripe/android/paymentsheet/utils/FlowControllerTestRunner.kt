package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FlowControllerTestRunnerContext(
    private val scenario: ActivityScenario<MainActivity>,
    private val flowController: PaymentSheet.FlowController,
) {

    fun configureFlowController(
        block: PaymentSheet.FlowController.() -> Unit,
    ) {
        scenario.onActivity {
            flowController.block()
        }
    }
}

internal fun runFlowControllerTest(
    createIntentCallback: CreateIntentCallback? = null,
    paymentOptionCallback: PaymentOptionCallback,
    resultCallback: PaymentSheetResultCallback,
    block: (FlowControllerTestRunnerContext) -> Unit,
) {
    runFlowControllerTest(
        createIntentCallback = createIntentCallback,
        paymentOptionCallback = paymentOptionCallback,
        resultCallback = resultCallback,
        integrationType = FlowControllerTestFactory.IntegrationType.Activity,
        block = block
    )
    runFlowControllerTest(
        createIntentCallback = createIntentCallback,
        paymentOptionCallback = paymentOptionCallback,
        resultCallback = resultCallback,
        integrationType = FlowControllerTestFactory.IntegrationType.Compose,
        block = block
    )
}

private fun runFlowControllerTest(
    createIntentCallback: CreateIntentCallback? = null,
    paymentOptionCallback: PaymentOptionCallback,
    resultCallback: PaymentSheetResultCallback,
    integrationType: FlowControllerTestFactory.IntegrationType,
    block: (FlowControllerTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    val factory = FlowControllerTestFactory(
        integrationType = integrationType,
        createIntentCallback = createIntentCallback,
        paymentOptionCallback = paymentOptionCallback,
        resultCallback = { result ->
            resultCallback.onPaymentSheetResult(result)
            countDownLatch.countDown()
        }
    )

    runFlowControllerTest(
        countDownLatch = countDownLatch,
        makeFlowController = factory::make,
        block = block,
    )
}

private fun runFlowControllerTest(
    countDownLatch: CountDownLatch,
    makeFlowController: (ComponentActivity) -> PaymentSheet.FlowController,
    block: (FlowControllerTestRunnerContext) -> Unit,
) {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.moveToState(Lifecycle.State.CREATED)

    scenario.onActivity {
        PaymentConfiguration.init(it, "pk_test_123")
    }

    lateinit var flowController: PaymentSheet.FlowController
    scenario.onActivity {
        flowController = makeFlowController(it)
    }

    scenario.moveToState(Lifecycle.State.RESUMED)

    val testContext = FlowControllerTestRunnerContext(scenario, flowController)
    block(testContext)

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    scenario.close()
}

package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.addresselement.AddressDetails
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
    scenario.onActivity { activity ->
        flowController = SynchronizedTestFlowController(makeFlowController(activity))
    }

    scenario.moveToState(Lifecycle.State.RESUMED)

    val testContext = FlowControllerTestRunnerContext(scenario, flowController)
    block(testContext)

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    scenario.close()
}

internal class SynchronizedTestFlowController(
    private val flowController: PaymentSheet.FlowController
) : PaymentSheet.FlowController by flowController, IdlingResource {
    private var isIdleNow: Boolean = false
    private var onIdleTransitionCallback: IdlingResource.ResourceCallback? = null

    init {
        IdlingRegistry.getInstance().register(this)
    }

    override fun presentPaymentOptions() {
        flowController.presentPaymentOptions()
        isIdleNow = true
        onIdleTransitionCallback?.onTransitionToIdle()
        IdlingRegistry.getInstance().unregister(this)
        onIdleTransitionCallback = null
    }

    override fun getName(): String = FLOW_CONTROLLER_RESOURCE

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        onIdleTransitionCallback = callback
    }

    override fun isIdleNow(): Boolean {
        return isIdleNow
    }

    companion object {
        private const val FLOW_CONTROLLER_RESOURCE = "FlowControllerResource"
    }
}

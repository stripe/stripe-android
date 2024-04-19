package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
    private val flowController: PaymentSheet.FlowController,
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
}

internal fun ActivityScenarioRule<MainActivity>.runFlowControllerTest(
    networkRule: NetworkRule,
    integrationType: IntegrationType,
    createIntentCallback: CreateIntentCallback? = null,
    paymentOptionCallback: PaymentOptionCallback,
    resultCallback: PaymentSheetResultCallback,
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
        networkRule = networkRule,
        countDownLatch = countDownLatch,
        makeFlowController = factory::make,
        block = block,
    )
}

private fun ActivityScenarioRule<MainActivity>.runFlowControllerTest(
    networkRule: NetworkRule,
    countDownLatch: CountDownLatch,
    makeFlowController: (ComponentActivity) -> PaymentSheet.FlowController,
    block: (FlowControllerTestRunnerContext) -> Unit,
) {
    scenario.moveToState(Lifecycle.State.CREATED)

    scenario.onActivity {
        PaymentConfiguration.init(it, "pk_test_123")
        LinkStore(it.applicationContext).clear()
    }

   var flowController: SynchronizedTestFlowController? = null

   try {
       scenario.onActivity { activity ->
           flowController = SynchronizedTestFlowController(makeFlowController(activity))
       }

       scenario.moveToState(Lifecycle.State.RESUMED)

       val testContext = FlowControllerTestRunnerContext(
           scenario = scenario,
           flowController = flowController ?: throw IllegalStateException(
               "FlowController should have been created!"
           )
       )
       block(testContext)

       val didCompleteSuccessfully = countDownLatch.await(5, TimeUnit.SECONDS)
       networkRule.validate()
       assertThat(didCompleteSuccessfully).isTrue()
   } catch (exception: Exception) {
       flowController?.manuallyRelease()

       throw exception
   }
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
        manuallyRelease()
    }

    override fun getName(): String = FLOW_CONTROLLER_RESOURCE

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        onIdleTransitionCallback = callback
    }

    override fun isIdleNow(): Boolean {
        return isIdleNow
    }

    fun manuallyRelease() {
        isIdleNow = true
        onIdleTransitionCallback?.onTransitionToIdle()
        IdlingRegistry.getInstance().unregister(this)
        onIdleTransitionCallback = null
    }

    companion object {
        private const val FLOW_CONTROLLER_RESOURCE = "FlowControllerResource"
    }
}

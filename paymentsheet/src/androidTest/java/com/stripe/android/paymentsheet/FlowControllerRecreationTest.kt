package com.stripe.android.paymentsheet

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.withTurbineTimeout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.ActivityLaunchObserver
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class FlowControllerRecreationTest {
    @get:Rule
    val testRules = TestRules.create {
        around(PaymentConfigurationTestRule(ApplicationProvider.getApplicationContext()))
    }

    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(testRules.compose)

    @Test
    fun onRecreationShouldNotEmitPreviouslyEmittedResults() = test {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val results = Turbine<PaymentSheetResult>()
        val activityLaunchObserver = ActivityLaunchObserver(PaymentOptionsActivity::class.java)
        val paymentOptionCallbackCountDownLatch = CountDownLatch(1)
        lateinit var flowController: PaymentSheet.FlowController

        onActivity { activity ->
            flowController = PaymentSheet.FlowController.Builder(
                paymentOptionCallback = {
                    paymentOptionCallbackCountDownLatch.countDown()
                },
                resultCallback = {
                    results.add(it)
                }
            ).build(activity = activity)

            activityLaunchObserver.prepareForLaunch(activity)

            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .build(),
                callback = { success, _ ->
                    assertThat(success).isTrue()
                    flowController.presentPaymentOptions()
                }
            )
        }

        activityLaunchObserver.awaitLaunch()

        page.fillOutCardDetails()
        page.clickPrimaryButton()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        assertThat(paymentOptionCallbackCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
        flowController.confirm()

        runTest {
            withTurbineTimeout(10.seconds) {
                assertThat(results.awaitItem()).isInstanceOf(PaymentSheetResult.Completed::class.java)
            }
        }

        recreate()

        val recreatedResults = Turbine<PaymentSheetResult>()
        lateinit var recreatedFlowController: PaymentSheet.FlowController

        onActivity { activity ->
            recreatedFlowController = PaymentSheet.FlowController.Builder(
                paymentOptionCallback = {
                    // No-op
                },
                resultCallback = {
                    recreatedResults.add(it)
                }
            ).build(activity = activity)
        }

        assertThat(recreatedFlowController).isNotNull()
        recreatedResults.expectNoEvents()
    }

    private fun test(block: ActivityScenario<MainActivity>.() -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            block(scenario)
        }
    }
}

package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CreateIntentResult
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
internal class FlowControllerTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun testSuccessfulCardPayment() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val resultCountDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var flowController: PaymentSheet.FlowController
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            flowController = PaymentSheet.FlowController.create(
                activity = it,
                paymentOptionCallback = { paymentOption ->
                    assertThat(paymentOption?.label).endsWith("4242")
                    flowController.confirm()
                },
                paymentResultCallback = { result ->
                    assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                    resultCountDownLatch.countDown()
                },
            )
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    flowController.presentPaymentOptions()
                }
            )
        }

        page.addPaymentMethod()
        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        assertThat(resultCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testFailedElementsSessionCall() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.setResponseCode(400)
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        val resultCountDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var flowController: PaymentSheet.FlowController
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            flowController = PaymentSheet.FlowController.create(
                activity = it,
                paymentOptionCallback = { paymentOption ->
                    assertThat(paymentOption?.label).endsWith("4242")
                    flowController.confirm()
                },
                paymentResultCallback = { result ->
                    assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                    resultCountDownLatch.countDown()
                },
            )
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    flowController.presentPaymentOptions()
                }
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        assertThat(resultCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testFailedConfirmCall() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val resultCountDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var flowController: PaymentSheet.FlowController
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            flowController = PaymentSheet.FlowController.create(
                activity = it,
                paymentOptionCallback = { paymentOption ->
                    assertThat(paymentOption?.label).endsWith("4242")
                    flowController.confirm()
                },
                paymentResultCallback = { result ->
                    assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
                    resultCountDownLatch.countDown()
                },
            )
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    flowController.presentPaymentOptions()
                }
            )
        }

        page.addPaymentMethod()
        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.setResponseCode(400)
        }

        page.clickPrimaryButton()

        assertThat(resultCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testActivityRecreationDoesNotMakeSubsequentCallsToElementsSession() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val paymentOptionCallbackCountDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        lateinit var flowController: PaymentSheet.FlowController

        fun initializeActivity() {
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                PaymentConfiguration.init(it, "pk_test_123")
                flowController = PaymentSheet.FlowController.create(
                    activity = it,
                    paymentOptionCallback = { paymentOption ->
                        assertThat(paymentOption?.label).endsWith("4242")
                        paymentOptionCallbackCountDownLatch.countDown()
                    },
                    paymentResultCallback = {
                        throw AssertionError("Not expected")
                    },
                )
            }
            scenario.moveToState(Lifecycle.State.RESUMED)
        }

        initializeActivity()
        scenario.onActivity {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    flowController.presentPaymentOptions()
                }
            )
        }

        page.addPaymentMethod()
        page.fillOutCardDetails()
        page.clickPrimaryButton()

        assertThat(paymentOptionCallbackCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()

        scenario.recreate()
        initializeActivity()

        val configureCallbackCountDownLatch = CountDownLatch(1)
        scenario.onActivity {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    assertThat(flowController.getPaymentOption()?.label).endsWith("4242")
                    configureCallbackCountDownLatch.countDown()
                }
            )
        }

        assertThat(configureCallbackCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testCallsElementsSessionsForSeparateConfiguredClientSecrets() {
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        lateinit var flowController: PaymentSheet.FlowController

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            flowController = PaymentSheet.FlowController.create(
                activity = it,
                paymentOptionCallback = {
                    throw AssertionError("Not expected")
                },
                paymentResultCallback = {
                    throw AssertionError("Not expected")
                },
            )
        }
        scenario.moveToState(Lifecycle.State.RESUMED)

        fun configureFlowController(paymentIntentClientSecret: String) {
            val countDownLatch = CountDownLatch(1)

            scenario.onActivity {
                flowController.configureWithPaymentIntent(
                    paymentIntentClientSecret = paymentIntentClientSecret,
                    configuration = null,
                    callback = { success, error ->
                        assertThat(success).isTrue()
                        assertThat(error).isNull()
                        countDownLatch.countDown()
                    }
                )
            }

            assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
            query("client_secret", "pi_example_secret_example"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }
        configureFlowController("pi_example_secret_example")

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
            query("client_secret", "pi_example2_secret_example2"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }
        configureFlowController("pi_example2_secret_example2")
    }

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    @Test
    fun testDeferredIntentCardPayment() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        val resultCountDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var flowController: PaymentSheet.FlowController
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            flowController = PaymentSheet.FlowController.create(
                activity = it,
                paymentOptionCallback = { paymentOption ->
                    assertThat(paymentOption?.label).endsWith("4242")
                    flowController.confirm()
                },
                createIntentCallback = {
                    CreateIntentResult.Success(
                        clientSecret = "pi_example_secret_example"
                    )
                },
                paymentResultCallback = { result ->
                    assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                    resultCountDownLatch.countDown()
                },
            )
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd"
                    )
                ),
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    flowController.presentPaymentOptions()
                }
            )
        }

        page.addPaymentMethod()
        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(
                bodyPart(
                    "payment_method_data%5Bpayment_user_agent%5D",
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        assertThat(resultCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    @Test
    fun testDeferredIntentFailedCardPayment() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        val resultCountDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var flowController: PaymentSheet.FlowController
        var paymentSheetResult: PaymentSheetResult? = null
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            flowController = PaymentSheet.FlowController.create(
                activity = it,
                paymentOptionCallback = { paymentOption ->
                    assertThat(paymentOption?.label).endsWith("4242")
                    flowController.confirm()
                },
                createIntentCallback = {
                    CreateIntentResult.Failure(
                        cause = Exception("We don't accept visa"),
                        displayMessage = "We don't accept visa"
                    )
                },
                paymentResultCallback = { result ->
                    assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
                    paymentSheetResult = result
                    resultCountDownLatch.countDown()
                },
            )
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd"
                    )
                ),
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    flowController.presentPaymentOptions()
                }
            )
        }

        page.addPaymentMethod()
        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()

        assertThat(resultCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat((paymentSheetResult as PaymentSheetResult.Failed).error.message)
            .isEqualTo("We don't accept visa")
    }
}

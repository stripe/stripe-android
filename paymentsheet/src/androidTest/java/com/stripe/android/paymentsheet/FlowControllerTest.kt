package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.ActivityLaunchObserver
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.assertFailed
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.testing.RetryRule
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(TestParameterInjector::class)
internal class FlowControllerTest {
    private val composeTestRule = createEmptyComposeRule()
    private val retryRule = RetryRule(5)
    private val networkRule = NetworkRule()

    @get:Rule
    val chain: RuleChain = RuleChain.emptyRuleChain()
        .around(DetectLeaksAfterTestSuccess())
        .around(composeTestRule)
        .around(retryRule)
        .around(networkRule)

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun testSuccessfulCardPayment(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configureFlowController {
            configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
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
    }

    @Test
    fun testFailedElementsSessionCall(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
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

        testContext.configureFlowController {
            configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
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
    }

    @Test
    fun testFailedConfirmCall(
        @TestParameter integrationType: IntegrationType,
    ) {
        runFlowControllerTest(
            networkRule = networkRule,
            integrationType = integrationType,
            paymentOptionCallback = { paymentOption ->
                assertThat(paymentOption?.label).endsWith("4242")
            },
            resultCallback = ::assertFailed,
        ) { testContext ->
            networkRule.enqueue(
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configureFlowController {
                configureWithPaymentIntent(
                    paymentIntentClientSecret = "pi_example_secret_example",
                    configuration = null,
                    callback = { success, error ->
                        assertThat(success).isTrue()
                        assertThat(error).isNull()
                        presentPaymentOptions()
                    }
                )
            }

            page.fillOutCardDetails()

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
            ) { response ->
                response.setResponseCode(400)
            }

            page.clickPrimaryButton()
        }
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var flowController: PaymentSheet.FlowController

        fun initializeActivity() {
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                PaymentConfiguration.init(it, "pk_test_123")

                val unsynchronizedController = PaymentSheet.FlowController.create(
                    activity = it,
                    paymentOptionCallback = { paymentOption ->
                        assertThat(paymentOption?.label).endsWith("4242")
                        paymentOptionCallbackCountDownLatch.countDown()
                    },
                    paymentResultCallback = {
                        throw AssertionError("Not expected")
                    },
                )

                flowController = unsynchronizedController
            }
            scenario.moveToState(Lifecycle.State.RESUMED)
        }

        initializeActivity()
        val activityLaunchObserver = ActivityLaunchObserver(PaymentOptionsActivity::class.java)
        scenario.onActivity {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    activityLaunchObserver.prepareForLaunch(it)
                    flowController.presentPaymentOptions()
                }
            )
        }

        activityLaunchObserver.awaitLaunch()

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
        val scenario = ActivityScenario.launch(MainActivity::class.java)
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

    @Test
    fun testDeferredIntentCardPayment(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, _ -> CreateIntentResult.Success("pi_example_secret_example") },
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        testContext.configureFlowController {
            configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(
                bodyPart(
                    urlEncode("payment_method_data[payment_user_agent]"),
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentFailedCardPayment(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Failure(
                cause = Exception("We don't accept visa"),
                displayMessage = "We don't accept visa"
            )
        },
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = { result ->
            assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
            assertThat((result as PaymentSheetResult.Failed).error.message)
                .isEqualTo("We don't accept visa")
        },
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        testContext.configureFlowController {
            configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()
    }

    @OptIn(DelicatePaymentSheetApi::class)
    @Test
    fun testDeferredIntentCardPaymentWithForcedSuccess(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success(PaymentSheet.IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT)
        },
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        testContext.configureFlowController {
            configureWithIntentConfiguration(
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
                    presentPaymentOptions()
                }
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentCardPaymentWithInvalidStripeIntent(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, _ -> CreateIntentResult.Success("pi_example_secret_example") },
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = { result ->
            val failureResult = result as? PaymentSheetResult.Failed
            assertThat(failureResult?.error?.message).isEqualTo(
                "Your PaymentIntent currency (usd) does not match " +
                    "the PaymentSheet.IntentConfiguration currency (cad)."
            )
        },
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        testContext.configureFlowController {
            configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        // This currency is different from USD in the created intent, which
                        // will cause the validator to fail this transaction.
                        amount = 5099,
                        currency = "cad",
                    )
                ),
                configuration = null,
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        page.clickPrimaryButton()
    }
}

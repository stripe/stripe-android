package com.stripe.android.paymentsheet

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.CustomerSessionApiPreview
import com.stripe.android.elements.payment.CreateIntentCallback
import com.stripe.android.elements.payment.DelicatePaymentSheetApi
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.PaymentMethodLayout
import com.stripe.android.elements.payment.WalletButtonsConfiguration
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.WalletButtonsPage
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.utils.ActivityLaunchObserver
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.MultipleInstancesTestType
import com.stripe.android.paymentsheet.utils.MultipleInstancesTestTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.assertFailed
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runMultipleFlowControllerInstancesTest
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_VIEW_MORE
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(TestParameterInjector::class)
internal class FlowControllerTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)
    private val walletButtonsPage = WalletButtonsPage(testRules.compose)

    private val defaultConfiguration = PaymentSheet.Configuration.Builder("Example, Inc.")
        .paymentMethodLayout(PaymentMethodLayout.Horizontal)
        .build()

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testSuccessfulCardPayment(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
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
                configuration = defaultConfiguration,
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

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @Test
    fun testSuccessfulCardPaymentWithVerticalMode(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
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
                configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                    .paymentMethodLayout(PaymentMethodLayout.Vertical)
                    .build(),
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        page.clickOnLpm("card", forVerticalMode = true)
        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @Test
    fun testCardRelaunchesIntoFormPage(
        @TestParameter integrationType: IntegrationType,
    ) {
        runFlowControllerTest(
            networkRule = networkRule,
            integrationType = integrationType,
            callConfirmOnPaymentOptionCallback = false,
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
                    configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                        .paymentMethodLayout(PaymentMethodLayout.Vertical)
                        .build(),
                    callback = { success, error ->
                        assertThat(success).isTrue()
                        assertThat(error).isNull()
                        presentPaymentOptions()
                    }
                )
            }

            page.clickOnLpm("card", forVerticalMode = true)
            page.fillOutCardDetails()

            page.clickPrimaryButton()
            testContext.consumePaymentOptionEventForFlowController("card", "4242")

            testContext.flowController.presentPaymentOptions()

            page.assertIsOnFormPage()

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testCashappRelaunchesIntoListPageWithCashappSelected(
        @TestParameter integrationType: IntegrationType,
    ) {
        runFlowControllerTest(
            networkRule = networkRule,
            integrationType = integrationType,
            callConfirmOnPaymentOptionCallback = false,
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
                    configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                        .paymentMethodLayout(PaymentMethodLayout.Vertical)
                        .build(),
                    callback = { success, error ->
                        assertThat(success).isTrue()
                        assertThat(error).isNull()
                        presentPaymentOptions()
                    }
                )
            }

            page.clickOnLpm("cashapp", forVerticalMode = true)
            page.assertLpmSelected("cashapp")

            page.clickPrimaryButton()
            testContext.consumePaymentOptionEventForFlowController("cashapp", "Cash App Pay")

            testContext.flowController.presentPaymentOptions()

            page.assertLpmSelected("cashapp")
            page.clickPrimaryButton()
            testContext.consumePaymentOptionEventForFlowController("cashapp", "Cash App Pay")

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testCorrectMandatesDisplayedAfterNavigation(
        @TestParameter integrationType: IntegrationType,
    ) {
        runFlowControllerTest(
            networkRule = networkRule,
            integrationType = integrationType,
            callConfirmOnPaymentOptionCallback = false,
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
                    intentConfiguration = IntentConfiguration(
                        mode = IntentConfiguration.Mode.Payment(
                            amount = 5099,
                            currency = "usd",
                            setupFutureUse = IntentConfiguration.SetupFutureUse.OffSession
                        )
                    ),
                    configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                        .build(),
                    callback = { success, error ->
                        assertThat(success).isTrue()
                        assertThat(error).isNull()
                        presentPaymentOptions()
                    }
                )
            }

            page.clickOnLpm("cashapp", forVerticalMode = true)
            page.assertLpmSelected("cashapp")
            page.assertHasMandate("By continuing, you authorize Example, Inc. to debit your Cash App account for this payment and future payments in accordance with Example, Inc.'s terms, until this authorization is revoked. You can change this anytime in your Cash App Settings.")

            page.clickOnLpm("card", forVerticalMode = true)
            page.waitForCardForm()
            page.assertHasMandate("By providing your card information, you allow Example, Inc. to charge your card for future payments in accordance with their terms.")

            Espresso.pressBack()

            page.assertLpmSelected("cashapp")
            page.assertHasMandate("By continuing, you authorize Example, Inc. to debit your Cash App account for this payment and future payments in accordance with Example, Inc.'s terms, until this authorization is revoked. You can change this anytime in your Cash App Settings.")

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testFailedElementsSessionCall(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
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
                configuration = defaultConfiguration,
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

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @Test
    fun testFailedConfirmCall(
        @TestParameter integrationType: IntegrationType,
    ) {
        runFlowControllerTest(
            networkRule = networkRule,
            integrationType = integrationType,
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
                    configuration = defaultConfiguration,
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

            testContext.consumePaymentOptionEventForFlowController("card", "4242")
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
        lateinit var flowController: FlowController

        fun initializeActivity() {
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                PaymentConfiguration.init(it, "pk_test_123")

                val unsynchronizedController = FlowController.Builder(
                    resultCallback = {
                        throw AssertionError("Not expected")
                    },
                    paymentOptionCallback = { paymentOption ->
                        assertThat(paymentOption?.label).endsWith("4242")
                        paymentOptionCallbackCountDownLatch.countDown()
                    }
                ).build(activity = it)

                flowController = unsynchronizedController
            }
            scenario.moveToState(Lifecycle.State.RESUMED)
        }

        initializeActivity()
        val activityLaunchObserver = ActivityLaunchObserver(PaymentOptionsActivity::class.java)
        scenario.onActivity {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
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
                configuration = defaultConfiguration,
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
        lateinit var flowController: FlowController

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            flowController = FlowController.Builder(
                resultCallback = {
                    throw AssertionError("Not expected")
                },
                paymentOptionCallback = {
                    throw AssertionError("Not expected")
                }
            ).build(activity = it)
        }
        scenario.moveToState(Lifecycle.State.RESUMED)

        fun configureFlowController(paymentIntentClientSecret: String) {
            val countDownLatch = CountDownLatch(1)

            scenario.onActivity {
                flowController.configureWithPaymentIntent(
                    paymentIntentClientSecret = paymentIntentClientSecret,
                    configuration = defaultConfiguration,
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
        builder = {
            createIntentCallback { _, _ ->
                CreateIntentCallback.Result.Success("pi_example_secret_example")
            }
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
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
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
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3Bdeferred-intent%3Bautopm")
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
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3Bdeferred-intent%3Bautopm")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @Test
    fun testDeferredIntentWithMultipleInstances(
        @TestParameter(valuesProvider = MultipleInstancesTestTypeProvider::class)
        testType: MultipleInstancesTestType,
    ) = runMultipleFlowControllerInstancesTest(
        networkRule = networkRule,
        testType = testType,
        createIntentCallback = { _, _ -> CreateIntentCallback.Result.Success("pi_example_secret_example") },
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
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
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
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @Test
    fun testDeferredIntentFailedCardPayment(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        builder = {
            createIntentCallback { _, _ ->
                CreateIntentCallback.Result.Failure(
                    cause = Exception("We don't accept visa"),
                    displayMessage = "We don't accept visa"
                )
            }
        },
        resultCallback = { result ->
            assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
            assertThat((result as PaymentSheetResult.Failed).error.cause?.message)
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
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
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
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @OptIn(DelicatePaymentSheetApi::class)
    @Test
    fun testDeferredIntentCardPaymentWithForcedSuccess(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        builder = {
            createIntentCallback { _, _ ->
                CreateIntentCallback.Result.Success(IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT)
            }
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
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
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
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @Test
    fun testDeferredIntentCardPaymentWithInvalidStripeIntent(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        builder = {
            createIntentCallback { _, _ ->
                CreateIntentCallback.Result.Success("pi_example_secret_example")
            }
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
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Payment(
                        // This currency is different from USD in the created intent, which
                        // will cause the validator to fail this transaction.
                        amount = 5099,
                        currency = "cad",
                    )
                ),
                configuration = defaultConfiguration,
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
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet.FlowController%3Bdeferred-intent%3Bautopm")
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

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
    }

    @Test
    fun testCvcRecollection(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_cvc_recollection.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success.json")
        }

        testContext.configureFlowController {
            configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                    paymentMethodLayout = PaymentMethodLayout.Horizontal,
                    allowsDelayedPaymentMethods = false,
                ),
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        composeTestRule.onNode(
            hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
                .and(hasText("4242", substring = true))
        ).performClick()

        val paymentOption = testContext.configureCallbackTurbine.awaitItem()
        assertThat(paymentOption?.label).endsWith("4242")
        assertThat(paymentOption?.paymentMethodType).isEqualTo("card")

        page.fillCvcRecollection("123")

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_options[card][cvc]"), "123")
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        composeTestRule
            .onNodeWithText("Confirm")
            .performClick()
    }

    @Test
    fun testSavedCardsInVerticalMode(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
        callConfirmOnPaymentOptionCallback = false,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_cvc_recollection.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success.json")
        }

        testContext.configureFlowController {
            configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                    .customer(
                        CustomerConfiguration(
                            id = "cus_1",
                            ephemeralKeySecret = "ek_123",
                        )
                    )
                    .paymentMethodLayout(PaymentMethodLayout.Vertical)
                    .allowsDelayedPaymentMethods(false)
                    .build(),
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag(TEST_TAG_VIEW_MORE).performClick()

        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_pm_67890").performClick()

        testContext.configureCallbackTurbine.expectNoEvents()
        page.clickPrimaryButton()
        testContext.consumePaymentOptionEventForFlowController("card", "4242")
        testContext.markTestSucceeded()
    }

    /**
     * Tests the default ordering of payment methods when Elements session fetch fails.
     *
     * This test verifies that:
     * 1. When Elements session request fails (400 response)
     * 2. And a subsequent payment intent fetch succeeds
     * 3. The payment methods are displayed in the expected default order:
     *    - Card
     *    - Afterpay/Clearpay
     *    - Klarna
     *    - Affirm
     *    Note: US Bank Account and Link are excluded based on configuration
     */
    @Test
    fun testDefaultPaymentMethodOrderWithFailedSession(
        @TestParameter integrationType: IntegrationType,
    ) = runFlowControllerTest(
        networkRule = networkRule,
        integrationType = integrationType,
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
                configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                    .allowsDelayedPaymentMethods(true)
                    .allowsPaymentMethodsRequiringShippingAddress(true)
                    .paymentMethodLayout(PaymentMethodLayout.Horizontal)
                    .build(),
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        val actualTags = composeTestRule
            .onNodeWithTag(TEST_TAG_LIST, true)
            .onChildren()
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }

        assertThat(actualTags).isEqualTo(
            listOf(
                "card",
                "afterpay_clearpay",
                "klarna",
                "us_bank_account",
            ).map { TEST_TAG_LIST + it }
        )

        // Scroll to check that Affirm is included, we expect it to be last in the list.
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).performScrollToIndex(4)
        composeTestRule.onNodeWithTag(TEST_TAG_LIST + "affirm").assertIsDisplayed()

        testContext.markTestSucceeded()
    }

    @OptIn(CustomerSessionApiPreview::class)
    @Test
    fun testWalletButtonsShown() = runFlowControllerTest(
        networkRule = networkRule,
        showWalletButtons = true,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val isConfigured = CountDownLatch(1)

        GooglePayRepository.googlePayAvailabilityClientFactory = object : GooglePayAvailabilityClient.Factory {
            override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                return object : GooglePayAvailabilityClient {
                    override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                        return true
                    }
                }
            }
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_pm_with_link_and_cs.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/customers/cus_1"),
        ) { response ->
            response.testBodyFromFile("customer-get-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        testContext.flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = "pi_123_secret_123",
            configuration = PaymentSheet.Configuration.Builder(
                merchantDisplayName = "Example, Inc."
            )
                .customer(
                    CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    )
                )
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .build(),
            callback = { _, _ ->
                isConfigured.countDown()
            },
        )

        isConfigured.await(5, TimeUnit.SECONDS)

        walletButtonsPage.assertLinkIsDisplayed()
        walletButtonsPage.assertGooglePayIsDisplayed()

        testContext.markTestSucceeded()
    }

    @OptIn(CustomerSessionApiPreview::class, WalletButtonsPreview::class)
    @Test
    fun testWalletsShownInExpectedScreensWhenFilteringWalletButtons() = runFlowControllerTest(
        networkRule = networkRule,
        showWalletButtons = true,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val isConfigured = CountDownLatch(1)

        GooglePayRepository.googlePayAvailabilityClientFactory = object : GooglePayAvailabilityClient.Factory {
            override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                return object : GooglePayAvailabilityClient {
                    override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                        return true
                    }
                }
            }
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_pm_with_link_and_cs.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/customers/cus_1"),
        ) { response ->
            response.testBodyFromFile("customer-get-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        val activityLaunchObserver = ActivityLaunchObserver(PaymentOptionsActivity::class.java)

        testContext.scenario.onActivity {
            activityLaunchObserver.prepareForLaunch(it)
            testContext.flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_123_secret_123",
                configuration = PaymentSheet.Configuration.Builder(
                    merchantDisplayName = "Example, Inc."
                )
                    .customer(
                        CustomerConfiguration.createWithCustomerSession(
                            id = "cus_1",
                            clientSecret = "cuss_123",
                        )
                    )
                    .googlePay(
                        GooglePayConfiguration(
                            environment = GooglePayConfiguration.Environment.Test,
                            countryCode = "US",
                        )
                    )
                    .walletButtons(
                        WalletButtonsConfiguration(
                            willDisplayExternally = true,
                            walletsToShow = listOf("link"),
                        )
                    )
                    .build(),
                callback = { _, _ ->
                    isConfigured.countDown()
                },
            )
        }

        isConfigured.await(5, TimeUnit.SECONDS)

        walletButtonsPage.assertGooglePayIsNotDisplayed()
        walletButtonsPage.assertLinkIsDisplayed()

        testContext.flowController.presentPaymentOptions()
        activityLaunchObserver.awaitLaunch()

        composeTestRule.waitForIdle()
        page.assertGooglePayIsDisplayed()

        testContext.markTestSucceeded()
    }
}

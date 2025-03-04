package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.PaymentSheetMultipleInstanceTestType
import com.stripe.android.paymentsheet.utils.PaymentSheetMultipleInstanceTestTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runMultiplePaymentSheetInstancesTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PaymentSheetDeferredTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    private val integrationType: IntegrationType = IntegrationType.Compose

    private val defaultConfiguration = PaymentSheet.Configuration(
        merchantDisplayName = "Example, Inc.",
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    @Test
    fun testDeferredIntentCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
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
                    urlEncode("payment_method_options[card][setup_future_usage]"),
                    "off_session"
                )
            ),
            not(
                bodyPart(
                    urlEncode("payment_method_data[payment_user_agent]"),
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentCardPayment_forSetup() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("seti_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup()
                ),
                configuration = defaultConfiguration,
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
            path("/v1/setup_intents/seti_example"),
        ) { response ->
            response.testBodyFromFile("setup-intent-get.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/setup_intents/seti_example/confirm"),
        ) { response ->
            response.testBodyFromFile("setup-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentCardPaymentWithCustomer() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "card"),
            query("customer", "cus_foobar"),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                    .customer(PaymentSheet.CustomerConfiguration("cus_foobar", "ek_test_foobar"))
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .build(),
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
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
                    urlEncode("payment_method_options[card][setup_future_usage]"),
                    "off_session"
                )
            ),
            not(
                bodyPart(
                    urlEncode("payment_method_data[payment_user_agent]"),
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentCardPaymentWithSaveFor() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isTrue()
            CreateIntentResult.Success("pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "card"),
            query("customer", "cus_foobar"),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
                    .customer(PaymentSheet.CustomerConfiguration("cus_foobar", "ek_test_foobar"))
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .build(),
            )
        }

        page.fillOutCardDetails()
        page.checkSaveForFuture()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
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
            bodyPart(
                urlEncode("payment_method_options[card][setup_future_usage]"),
                "off_session"
            ),
            not(
                bodyPart(
                    urlEncode("payment_method_data[payment_user_agent]"),
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentFailedCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Failure(
                cause = Exception("We don't accept visa"),
                displayMessage = "We don't accept visa"
            )
        },
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()

        page.waitForText("We don't accept visa")
        testContext.markTestSucceeded()
    }

    @OptIn(DelicatePaymentSheetApi::class)
    @Test
    fun testDeferredIntentCardPaymentWithForcedSuccess() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success(PaymentSheet.IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT)
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentKonbiniPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = PaymentSheet.Configuration.Builder("Example, Inc")
                    .allowsDelayedPaymentMethods(true)
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .build(),
            )
        }

        page.clickOnLpm("konbini")
        val phone = "1234567890"
        page.fillOutKonbini(
            fullName = "John Doe",
            email = "email@email.com",
            phone = phone,
        )

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
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
            bodyPart(
                urlEncode("payment_method_options[konbini][confirmation_number]"),
                phone
            ),
            not(
                bodyPart(
                    urlEncode("payment_method_data[payment_user_agent]"),
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredPaymentIntent_withElementsSessionFailure() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.setResponseCode(400)
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
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
                    urlEncode("payment_method_options[card][setup_future_usage]"),
                    "off_session"
                )
            ),
            not(
                bodyPart(
                    urlEncode("payment_method_data[payment_user_agent]"),
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent%3Bautopm")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredSetupIntent_withElementsSessionFailure() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("seti_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.setResponseCode(400)
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup()
                ),
                configuration = defaultConfiguration,
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
            path("/v1/setup_intents/seti_example"),
        ) { response ->
            response.testBodyFromFile("setup-intent-get.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/setup_intents/seti_example/confirm"),
        ) { response ->
            response.testBodyFromFile("setup-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentWithMultipleInstances(
        @TestParameter(valuesProvider = PaymentSheetMultipleInstanceTestTypeProvider::class)
        testType: PaymentSheetMultipleInstanceTestType,
    ) = runMultiplePaymentSheetInstancesTest(
        networkRule = networkRule,
        testType = testType,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success(clientSecret = "pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = defaultConfiguration,
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
    }
}

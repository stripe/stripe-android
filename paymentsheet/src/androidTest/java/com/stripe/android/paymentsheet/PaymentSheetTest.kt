package com.stripe.android.paymentsheet

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onAllNodesWithTag
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.ui.TEST_TAG_MODIFY_BADGE
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.assertFailed
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PaymentSheetTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    private val defaultConfiguration = PaymentSheet.Configuration(
        merchantDisplayName = "Example, Inc.",
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    @Test
    fun testSuccessfulCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
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
    fun testSocketErrorCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST
        }

        page.clickPrimaryButton()
        page.waitForText("An error occurred. Check your connection and try again.")
        page.assertNoText("IOException", substring = true)
        testContext.markTestSucceeded()
    }

    @Test
    fun testInsufficientFundsCardPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::expectNoResult,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.setResponseCode(402)
            response.testBodyFromFile("payment-intent-confirm-insufficient-funds.json")
        }

        page.clickPrimaryButton()
        page.waitForText("Your card has insufficient funds.")
        page.assertNoText("StripeException", substring = true)
        testContext.markTestSucceeded()
    }

    @Test
    fun testSuccessfulDelayedSuccessPayment() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        successTimeoutSeconds = 10L,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm_with-requires_action-status.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-success.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testFailureWhenSetupRequestsFail() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertFailed,
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
            response.setResponseCode(400)
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
            )
        }
    }

    @Test
    fun testPaymentIntentWithCardBrandChoiceSuccess() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method_with_cbc.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetailsWithCardBrandChoice()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(
                urlEncode("payment_method_data[card][networks][preferred]"),
                "cartes_bancaires"
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testPaymentIntentReturnsFailureWhenAlreadySucceeded() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertFailed,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-payment_intent_success.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
            )
        }
    }

    @Test
    fun testCardMetadataQueryExecutedOncePerCardSessionForBin() {
        repeat(2) {
            runPaymentSheetTest(
                networkRule = networkRule,
                integrationType = integrationType,
                resultCallback = ::assertCompleted,
            ) { testContext ->
                networkRule.enqueue(
                    host("api.stripe.com"),
                    method("GET"),
                    path("/v1/elements/sessions"),
                ) { response ->
                    response.testBodyFromFile("elements-sessions-requires_payment_method_with_cbc.json")
                }

                testContext.presentPaymentSheet {
                    presentWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = defaultConfiguration,
                    )
                }

                networkRule.enqueue(
                    method("GET"),
                    path("edge-internal/card-metadata")
                ) { response ->
                    response.testBodyFromFile("card-metadata-get.json")
                }

                page.fillOutCardDetails()
                page.clearCard()
                page.fillCard()
                page.clearCard()
                page.fillCard()

                networkRule.enqueue(
                    method("POST"),
                    path("/v1/payment_intents/pi_example/confirm")
                ) { response ->
                    response.testBodyFromFile("payment-intent-confirm.json")
                }

                page.clickPrimaryButton()
            }
        }
    }

    @Test
    fun testPaymentIntentWithCvcRecollection() = runPaymentSheetTest(
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

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
            )
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_options[card][cvc]"), "123")
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.fillCvcRecollection("123")
        page.clickPrimaryButton()
    }

    @Test
    fun testPrimaryButtonAccessibility() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        page.assertPrimaryButton(
            expectedContentDescription = "Pay \$50.99",
            canPay = true
        )

        page.clearCard()

        page.assertPrimaryButton(
            expectedContentDescription = "Pay \$50.99",
            canPay = false
        )

        testContext.markTestSucceeded()
    }

    @Test
    fun testFocusFirstEditBadgeOnEdit() = runPaymentSheetTest(
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

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
            )
        }

        page.clickEditButton()

        // Check that the first badge is focused
        composeTestRule
            .onAllNodesWithTag(TEST_TAG_MODIFY_BADGE)[0]
            .assertIsFocused()

        testContext.markTestSucceeded()
    }
}

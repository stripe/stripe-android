package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.assertFailed
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class PaymentSheetTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun testSuccessfulCardPayment() = runPaymentSheetTest(
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
                configuration = null,
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
                configuration = null,
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
                configuration = null,
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

    // TODO: Add a test for

    @Test
    fun testSuccessfulDelayedSuccessPayment() = runPaymentSheetTest(
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
                configuration = null,
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
                configuration = null,
            )
        }
    }

    @Test
    fun testDeferredIntentCardPayment() = runPaymentSheetTest(
        createIntentCallback = { _, _ -> CreateIntentResult.Success("pi_example_secret_example") },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5099,
                        currency = "usd"
                    )
                ),
                configuration = null,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent")
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
                    "payment_method_data%5Bpayment_user_agent%5D",
                    Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent")
                )
            ),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testDeferredIntentFailedCardPayment() = runPaymentSheetTest(
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
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd"
                    )
                ),
                configuration = null,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent")
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
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success(PaymentSheet.IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT)
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent.json")
        }

        testContext.presentPaymentSheet {
            presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd"
                    )
                ),
                configuration = null,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
            bodyPart(
                "payment_user_agent",
                Regex("stripe-android%2F\\d*.\\d*.\\d*%3BPaymentSheet%3Bdeferred-intent")
            ),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        page.clickPrimaryButton()
    }
}

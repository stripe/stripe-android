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
import com.stripe.android.networktesting.testBodyFromFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
internal class PaymentSheetTest {
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

        val countDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(it) { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                countDownLatch.countDown()
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithPaymentIntent(
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

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testSuccessfulDelayedSuccessPayment() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        val countDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(it) { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                countDownLatch.countDown()
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithPaymentIntent(
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

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testFailureWhenSetupRequestsFail() {
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

        val countDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(it) { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
                countDownLatch.countDown()
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = null,
            )
        }

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
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

        val countDownLatch = CountDownLatch(1)
        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(
                activity = it,
                createIntentCallback = {
                    CreateIntentResult.Success("pi_example_secret_example")
                }
            ) { result ->
                assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
                countDownLatch.countDown()
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithIntentConfiguration(
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

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
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

        val activityScenarioRule = composeTestRule.activityRule
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        lateinit var paymentSheet: PaymentSheet
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentSheet = PaymentSheet(
                activity = it,
                createIntentCallback = {
                    CreateIntentResult.Failure(
                        cause = Exception("We don't accept visa"),
                        displayMessage = "We don't accept visa"
                    )
                }
            ) {
                error("Shouldn't call PaymentSheetResultCallback")
            }
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity {
            paymentSheet.presentWithIntentConfiguration(
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
    }
}

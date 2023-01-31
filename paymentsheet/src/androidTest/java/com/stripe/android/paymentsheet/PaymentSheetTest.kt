package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
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

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun testCompleteFlowWithSuccessfulCardPayment() {
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

        fillOutCard()

        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get.json")
        }

        onView(withId(R.id.primary_button)).perform(scrollTo(), click())

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testCustomFlowWithSuccessfulCardPayment() {
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

        composeTestRule.waitUntil {
            composeTestRule.onAllNodes(hasText("+ Add"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasText("+ Add"))
            .onParent()
            .onParent()
            .performClick()

        fillOutCard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get.json")
        }

        onView(withId(R.id.primary_button)).perform(scrollTo(), click())

        assertThat(resultCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    private fun fillOutCard() {
        composeTestRule.waitUntil {
            composeTestRule.onAllNodes(hasText("Card number"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasText("Card number"))
            .performTextReplacement("4242424242424242")
        composeTestRule.onNode(hasText("MM / YY"))
            .performTextReplacement("12/34")
        composeTestRule.onNode(hasText("CVC"))
            .performTextReplacement("123")
        composeTestRule.onNode(hasText("ZIP Code"))
            .performTextReplacement("12345")
    }
}

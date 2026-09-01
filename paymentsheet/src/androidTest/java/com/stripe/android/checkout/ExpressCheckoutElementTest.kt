package com.stripe.android.checkout

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
            .around(IntentsRule())
    }

    @Test
    fun testSuccessfulGooglePayPayment() {
        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            setup = { controller ->
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    configuration = CheckoutController.Configuration()
                        .expressCheckoutElement(ExpressCheckoutElement.Configuration())
                ).getOrThrow()
                assertThat(controller.session.value?.availableExpressCheckoutPaymentMethods).hasSize(1)
            },
        ) {
            testRules.compose.waitUntil(timeoutMillis = 5_000) {
                testRules.compose.onAllNodes(hasTestTag(GOOGLE_PAY_BUTTON_TEST_TAG))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            val paymentMethod = PaymentMethodFactory.card()

            intending(hasComponent(GOOGLE_PAY_ACTIVITY_NAME)).respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(
                        "extra_result",
                        GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod),
                    ),
                )
            )

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            testRules.compose.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).performClick()
        }

        intended(hasComponent(GOOGLE_PAY_ACTIVITY_NAME))
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        // Link looks up the consumer during both the initial load and the post-confirmation refresh.
        repeat(2) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
        }
        // Native Link confirms in its activity, so the controller refreshes the session after receiving the result.
        networkRule.checkoutInit(responseFactory = ::nativeLinkCheckoutInitResponse)

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            checkoutInitResponse = ::nativeLinkCheckoutInitResponse,
            setup = { controller ->
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    configuration = CheckoutController.Configuration()
                        .expressCheckoutElement(ExpressCheckoutElement.Configuration())
                ).getOrThrow()
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                        ?.filterIsInstance<ExpressCheckoutElement.PaymentMethod.Link>()
                ).hasSize(1)
            },
        ) {
            testRules.compose.waitUntil(timeoutMillis = 5_000) {
                testRules.compose.onAllNodes(hasTestTag(LinkButtonTestTag))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            intending(hasComponent(LinkActivity::class.java.name)).respondWith(
                Instrumentation.ActivityResult(
                    LinkActivity.RESULT_COMPLETE,
                    Intent().putExtra(
                        LinkActivityContract.EXTRA_RESULT,
                        LinkActivityResult.Completed(LinkAccountUpdate.None),
                    ),
                )
            )

            testRules.compose.onNodeWithTag(LinkButtonTestTag).performClick()
        }

        intended(hasComponent(LinkActivity::class.java.name))
    }

    private fun nativeLinkCheckoutInitResponse(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.put("account_settings", JSONObject().put("country", "US"))
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val GOOGLE_PAY_ACTIVITY_NAME =
            "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"
    }
}

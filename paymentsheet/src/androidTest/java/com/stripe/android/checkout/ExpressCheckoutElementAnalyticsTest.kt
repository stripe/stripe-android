package com.stripe.android.checkout

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds, // Analytics requests happen async.
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(AdvancedFraudSignalsTestRule())
            .around(GooglePayRepositoryTestRule())
            .around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
            .around(IntentsRule())
    }

    private val page = ExpressCheckoutElementPage(testRules.compose)

    @Test
    fun testSuccessfulGooglePayPayment() {
        validateInitialAnalyticsRequests()

        // This is called twice during load
        repeat(2) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.GooglePay()
                )
            },
        ) {
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
            validateAnalyticsRequest(
                eventName = "mc_ece_wallet_tapped",
                analyticsPayloadField("selected_lpm", "google_pay"),
            )
            repeat(2) {
                validateAnalyticsRequest(eventName = "mc_load_started")
            }
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_success",
                analyticsPayloadField("selected_lpm", "google_pay"),
            )
            repeat(2) {
                validateAnalyticsRequest(eventName = "mc_load_succeeded")
            }

            page.clickGooglePayButton()
        }

        intended(hasComponent(GOOGLE_PAY_ACTIVITY_NAME))
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        validateInitialAnalyticsRequests()

        // This is called twice during load
        repeat(2) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.Link()
                )
            },
        ) {
            intending(hasComponent(LinkActivity::class.java.name)).respondWith(
                Instrumentation.ActivityResult(
                    LinkActivity.RESULT_COMPLETE,
                    Intent().putExtra(
                        LinkActivityContract.EXTRA_RESULT,
                        LinkActivityResult.Completed(LinkAccountUpdate.None),
                    ),
                )
            )

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
            networkRule.checkoutInit(responseFactory = CheckoutInitResponseFactory::create)
            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(
                eventName = "mc_ece_wallet_tapped",
                analyticsPayloadField("selected_lpm", "link"),
                analyticsPayloadField("link_context", "wallet"),
            )
            validateAnalyticsRequest(eventName = "link.popup.success")
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_success",
                analyticsPayloadField("selected_lpm", "link"),
                analyticsPayloadField("link_context", "wallet"),
            )
            repeat(2) {
                validateAnalyticsRequest(eventName = "mc_load_started")
                validateAnalyticsRequest(eventName = "link.account_lookup.complete")
                validateAnalyticsRequest(eventName = "mc_load_succeeded")
            }

            page.clickLinkButton()
        }

        intended(hasComponent(LinkActivity::class.java.name))
    }

    private fun validateInitialAnalyticsRequests() {
        repeat(2) {
            validateAnalyticsRequest(eventName = "mc_load_started")
        }
        repeat(2) {
            validateAnalyticsRequest(eventName = "link.account_lookup.complete")
        }
        repeat(2) {
            validateAnalyticsRequest(eventName = "mc_load_succeeded")
        }
        validateAnalyticsRequest(eventName = "mc_ece_init")
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("Checkout"),
            *requestMatchers,
        )
    }

    private companion object {
        const val GOOGLE_PAY_ACTIVITY_NAME =
            "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"
    }
}

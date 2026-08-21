package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(AdvancedFraudSignalsTestRule())
            .around(GooglePayRepositoryTestRule())
    }

    private val contentPage = EmbeddedContentPage(testRules.compose)

    @Test
    fun testTotalChangeFailureSendsAnalyticsErrorCode() {
        val initialTotal = 5099
        val updatedTotal = 5399
        // Initial configuration and the post-failure session refresh each report loading.
        repeat(2) {
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("Checkout"),
            )
        }
        networkRule.validateAnalyticsRequest(
            eventName = "mc_initial_displayed_payment_methods",
            productUsage = setOf("Checkout"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_embedded_payment_failure",
            productUsage = setOf("Checkout"),
            analyticsPayloadField("error_message", "checkoutSessionTotalChanged"),
            analyticsPayloadField("error_code", "checkout_session_total_changed"),
        )
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val failure = result as CheckoutController.Result.Failed
                assertThat(failure.error).isInstanceOf(LocalStripeException::class.java)
            },
            checkoutInitResponse = { response ->
                response.testBodyFromFile("checkout-session-init.json") { json ->
                    json.put("customer_email", "checkout@example.com")
                    json.getJSONObject("elements_session").remove("link_settings")
                    json.put("account_settings", JSONObject("""{"country":"US"}"""))
                    json.getJSONObject("total_summary").apply {
                        put("subtotal", initialTotal)
                        put("due", initialTotal)
                        put("total", initialTotal)
                    }
                    json.put(
                        "tax_context",
                        JSONObject(
                            """
                            {
                                "automatic_tax_enabled": true,
                                "automatic_tax_address_source": "billing"
                            }
                            """.trimIndent()
                        )
                    )
                    json.put(
                        "customer",
                        JSONObject(
                            """
                            {
                                "id": "cus_123",
                                "payment_methods": [{
                                    "id": "pm_12345",
                                    "object": "payment_method",
                                    "type": "card",
                                    "billing_details": {
                                        "address": {
                                            "country": "US",
                                            "postal_code": "12345"
                                        }
                                    },
                                    "card": {
                                        "brand": "visa",
                                        "exp_month": 12,
                                        "exp_year": 2034,
                                        "last4": "4242"
                                    }
                                }],
                                "can_detach_payment_method": true
                            }
                            """.trimIndent()
                        )
                    )
                }
            },
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) { context ->
            contentPage.assertHasSelectedSavedPaymentMethod("pm_12345")
            networkRule.checkoutUpdate { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONObject("total_summary").put("total", updatedTotal)
                }
            }
            networkRule.checkoutInit { response ->
                response.testBodyFromFile("checkout-session-init.json") { json ->
                    json.put("account_settings", JSONObject("""{"country":"US"}"""))
                }
            }

            context.confirm()
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}

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
import com.stripe.android.paymentelement.EmbeddedFormPage
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
    private val formPage = EmbeddedFormPage(testRules.compose)

    @Test
    fun testSheetAnalyticsUsesCheckoutProductUsage() = runCheckoutPaymentElementTest(
        networkRule = networkRule,
        setup = { controller ->
            networkRule.validateAnalyticsRequest(
                eventName = "mc_embedded_init",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_initial_displayed_payment_methods",
                productUsage = setOf("Checkout"),
            )
            controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
        },
    ) { context ->
        networkRule.validateAnalyticsRequest(
            eventName = "mc_carousel_payment_method_tapped",
            productUsage = setOf("Checkout"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "stripe_android.card_metadata_pk_available",
            productUsage = setOf("Checkout"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_form_shown",
            productUsage = setOf("Checkout"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "stripe_android.card_metadata_pk_available",
            productUsage = setOf("Checkout"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_cardscan_api_check_failed",
            productUsage = setOf("Checkout"),
        )

        contentPage.clickOnLpm("card")
        formPage.waitUntilVisible()
        context.markTestSucceeded()
    }

    @Test
    fun testTotalChangeFailureSendsAnalyticsErrorCode() = runCheckoutPaymentElementTest(
        networkRule = networkRule,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
            val failure = result as CheckoutController.Result.Failed
            assertThat(failure.error).isInstanceOf(LocalStripeException::class.java)
        },
        checkoutInitResponse = { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("account_settings", JSONObject("""{"country":"US"}"""))
                json.getJSONObject("total_summary").apply {
                    put("subtotal", INITIAL_TOTAL)
                    put("total", INITIAL_TOTAL)
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
            networkRule.validateAnalyticsRequest(
                eventName = "mc_embedded_init",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_initial_displayed_payment_methods",
                productUsage = setOf("Checkout"),
            )
            controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
        },
    ) { context ->
        contentPage.assertHasSelectedSavedPaymentMethod("pm_12345")
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.getJSONObject("total_summary").put("total", UPDATED_TOTAL)
            }
        }
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("account_settings", JSONObject("""{"country":"US"}"""))
            }
        }
        networkRule.validateAnalyticsRequest(
            eventName = "mc_embedded_payment_failure",
            productUsage = setOf("Checkout"),
            analyticsPayloadField("error_message", "checkoutSessionTotalChanged"),
            analyticsPayloadField("error_code", "checkout_session_total_changed"),
        )
        // The Checkout Session is refreshed after confirmation fails.
        networkRule.validateAnalyticsRequest(
            eventName = "mc_embedded_init",
            productUsage = setOf("Checkout"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_load_started",
            productUsage = setOf("Checkout"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_load_succeeded",
            productUsage = setOf("Checkout"),
        )

        context.confirm()
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val INITIAL_TOTAL = 5099
        const val UPDATED_TOTAL = 5399
    }
}

package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
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
            .around(IntentsRule())
    }

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    @Test
    fun testSheetAnalyticsUsesCheckoutProductUsage() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        setup = { controller ->
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
    ) {
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
        markTestSucceeded()
    }

    @Test
    fun testGooglePayTotalChangeFailureSendsAnalyticsErrorCode() {
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val failure = result as CheckoutController.Result.Failed
                assertThat(failure.error).isInstanceOf(LocalStripeException::class.java)
            },
            checkoutInitResponse = ::billingTaxCheckoutInitResponse,
            setup = { controller ->
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
        ) {
            // Google Pay returns a new payment method, so its billing address is only synced to
            // the Checkout Session during confirmation.
            enqueueSuccessfulGooglePayPayment(paymentMethod = createPaymentMethodWithBillingAddress())
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[line1]", "510 Townsend St"),
                bodyPart("tax_region[line2]", "Floor 3"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONArray("checkout_items").getJSONObject(0)
                        .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                        .put("total", UPDATED_TOTAL)
                }
            }
            networkRule.checkoutInit(responseFactory = ::billingTaxCheckoutInitResponse)
            networkRule.validateAnalyticsRequest(
                eventName = "mc_embedded_payment_failure",
                productUsage = setOf("Checkout"),
                analyticsPayloadField("selected_lpm", "google_pay"),
                analyticsPayloadField("error_message", "checkoutSessionTotalChanged"),
                analyticsPayloadField("error_code", "checkout_session_total_changed"),
            )
            // The Checkout Session is refreshed after confirmation fails.
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("Checkout"),
            )

            contentPage.clickOnLpm("google_pay")
            confirm()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testSelectingSavedPaymentMethodWithoutBillingAddressReportsUnexpectedError() {
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            checkoutInitResponse = { response ->
                response.testBodyFromFile("checkout-session-init.json") { json ->
                    json.getJSONObject("elements_session").remove("link_settings")
                    json.put(
                        "tax_context",
                        JSONObject()
                            .put("automatic_tax_enabled", true)
                            .put("automatic_tax_address_source", "billing"),
                    )
                }
            },
            setup = { controller ->
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
        ) {
            contentPage.waitUntilVisible()
            networkRule.validateAnalyticsRequest(
                eventName = "unexpected_error.checkout.saved_payment_method.missing_billing_address",
                productUsage = setOf("Checkout"),
            )
            // Changing the selection reloads the payment element.
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("Checkout"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("Checkout"),
            )

            // Billing-tax filtering keeps addressless saved payment methods out of the UI, so
            // select one directly.
            val addresslessPaymentMethod = requireNotNull(
                PaymentMethodJsonParser().parse(
                    JSONObject(
                        """
                        {
                            "id": "pm_addressless",
                            "object": "payment_method",
                            "type": "card",
                            "card": {
                                "brand": "visa",
                                "exp_month": 12,
                                "exp_year": 2034,
                                "last4": "4242"
                            }
                        }
                        """.trimIndent()
                    )
                )
            )
            withContext(Dispatchers.Main) {
                controller.selectSavedPaymentMethod(PaymentSelection.Saved(addresslessPaymentMethod))
                    .getOrThrow()
            }
            markTestSucceeded()
        }
    }

    private fun billingTaxCheckoutInitResponse(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.put("account_settings", JSONObject().put("country", "US"))
            json.getJSONObject("elements_session").remove("link_settings")
            // Google Pay only returns a full billing address when billing collection is required.
            json.put("billing_address_collection", "required")
            json.put(
                "tax_context",
                JSONObject()
                    .put("automatic_tax_enabled", true)
                    .put("automatic_tax_address_source", "session.billing"),
            )
            json.put(
                "tax_meta",
                JSONObject()
                    .put("computation_type", "automatic")
                    .put("status", "requires_location_inputs"),
            )
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val UPDATED_TOTAL = 5399
    }
}

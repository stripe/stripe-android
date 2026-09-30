package com.stripe.android.checkout

import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import kotlinx.coroutines.runBlocking
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
    fun testSelectingSavedPaymentMethodWithoutBillingAddressReportsUnexpectedError() {
        lateinit var controller: CheckoutController
        runCheckoutPaymentElementTest(
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
            setup = { configuredController ->
                controller = configuredController
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
            runBlocking {
                controller.selectSavedPaymentMethod(PaymentSelection.Saved(addresslessPaymentMethod))
                    .getOrThrow()
            }
            context.markTestSucceeded()
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}

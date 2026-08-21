package com.stripe.android.checkout

import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.checkouttesting.createPaymentMethod
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.android.paymentsheet.utils.TestRules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    @Before
    fun setup() {
        AnalyticsRequestExecutor.ENABLED = false
    }

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testBackingOutOfFormPreservesPreviouslySelectedPaymentMethod() {
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) { context ->
            // Open the card form, then back out without entering any details.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Select a payment method that does not require a form.
            contentPage.clickOnLpm("cashapp")
            contentPage.assertHasSelectedLpm("cashapp")

            // Re-open the card form and back out again.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Backing out of the form must not clear the previously selected payment method.
            contentPage.assertHasSelectedLpm("cashapp")
            context.markTestSucceeded()
        }
    }

    @Test
    fun testSuccessfulCardPayment() {
        var checkoutResult: CheckoutController.Result? = null
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            resultCallback = { result -> checkoutResult = result },
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) { context ->
            networkRule.createPaymentMethod()
            networkRule.checkoutConfirm { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            contentPage.clickOnLpm("card")
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            context.confirm()
        }

        assertThat(checkoutResult).isInstanceOf(CheckoutController.Result.Completed::class.java)
    }

    @Test
    fun testTotalChangeFailureSendsAnalyticsErrorCode() {
        var checkoutResult: CheckoutController.Result? = null
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            resultCallback = { result -> checkoutResult = result },
            checkoutInitResponse = { response ->
                response.testBodyFromFile("checkout-session-init.json") { json ->
                    json.put("customer_email", "checkout@example.com")
                    json.getJSONObject("elements_session").remove("link_settings")
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
                    json.getJSONObject("total_summary").put("total", 5399)
                }
            }
            networkRule.checkoutInit { response ->
                response.testBodyFromFile("checkout-session-init.json")
            }
            networkRule.validateAnalyticsRequest(
                eventName = "mc_embedded_payment_failure",
                productUsage = setOf("Checkout"),
                analyticsPayloadField("error_message", "checkoutSessionTotalChanged"),
                analyticsPayloadField("error_code", "checkout_session_total_changed"),
            )

            AnalyticsRequestExecutor.ENABLED = true
            context.confirm()
        }

        assertThat(checkoutResult).isInstanceOf(CheckoutController.Result.Failed::class.java)
        val failure = checkoutResult as CheckoutController.Result.Failed
        assertThat(failure.error).isInstanceOf(LocalStripeException::class.java)
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}

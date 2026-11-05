package com.stripe.android.checkout

import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.validateAnalyticsRequest
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
    fun testSheetAnalyticsUsesCheckoutElementsProductUsageAndCheckoutSessionId() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            networkRule.validateAnalyticsRequest(
                eventName = "mc_initial_displayed_payment_methods",
                productUsage = setOf("checkout_elements"),
                analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            )
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            )
                .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "mc_carousel_payment_method_tapped",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_form_shown",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "stripe_android.card_metadata_pk_available",
            productUsage = setOf("checkout_elements"),
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_cardscan_api_check_failed",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
        )

        contentPage.clickOnLpm("card")
        formPage.waitUntilVisible()

        networkRule.validateAnalyticsRequest(
            eventName = "mc_dismiss",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
        )
        Espresso.pressBack()
        formPage.waitUntilMissing()
        markTestSucceeded()
    }

    @Test
    fun testGooglePayTotalChangeFailureSendsAnalyticsErrorCode() {
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutElementsController.Result.Failed::class.java)
                val failure = result as CheckoutElementsController.Result.Failed
                assertThat(failure.error).isInstanceOf(LocalStripeException::class.java)
            },
            checkoutInitResponse = ::billingTaxCheckoutInitResponse,
            setup = { controller ->
                networkRule.validateCheckoutInitStartedAndSucceeded()
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_initial_displayed_payment_methods",
                    productUsage = setOf("checkout_elements"),
                    analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
                )
                assertThat(
                    controller.configure(
                        DEFAULT_CLIENT_SECRET,
                        CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                    )
                )
                    .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
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
                    json.getJSONArray("items").getJSONObject(0)
                        .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                        .getJSONObject("amount_details")
                        .put("total", UPDATED_TOTAL)
                }
            }
            networkRule.checkoutInitWithAnalytics(responseFactory = ::billingTaxCheckoutInitResponse)
            networkRule.validateCheckoutAnalyticsStarted(
                "confirm",
                analyticsPayloadField("invocation_source", "merchant"),
                analyticsPayloadField("selected_lpm", "google_pay"),
            )
            networkRule.validateCheckoutAnalyticsFailed(
                "confirm",
                analyticsPayloadField("invocation_source", "merchant"),
                analyticsPayloadField("selected_lpm", "google_pay"),
                analyticsPayloadField("error_type", "checkoutSessionTotalChanged"),
                analyticsPayloadField("error_code", "checkout_session_total_changed"),
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
                    json.remove("link_settings")
                    json.put(
                        "automatic_tax",
                        JSONObject()
                            .put("enabled", true)
                            .put("status", "ready")
                            .put("address_source", "billing"),
                    )
                }
            },
            setup = { controller ->
                networkRule.validateCheckoutInitStartedAndSucceeded()
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_initial_displayed_payment_methods",
                    productUsage = setOf("checkout_elements"),
                    analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
                )
                assertThat(
                    controller.configure(
                        DEFAULT_CLIENT_SECRET,
                        CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                    )
                )
                    .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
            },
        ) {
            contentPage.waitUntilVisible()
            networkRule.validateAnalyticsRequest(
                eventName = "unexpected_error.checkout.saved_payment_method.missing_billing_address",
                productUsage = setOf("checkout_elements"),
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

    @Test
    fun testSelectingSavedPaymentMethodWithBillingAutomaticTaxReportsTaxUpdate() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        checkoutInitResponse = ::billingTaxCheckoutInitResponse,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateCheckoutAnalyticsStarted(
            "update_billing_address",
            analyticsPayloadField("invocation_source", "payment_element"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", "510 Townsend St"),
            bodyPart("tax_region[line2]", "Floor 3"),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94103"),
        ) { response ->
            billingTaxCheckoutInitResponse(response)
        }
        networkRule.validateCheckoutAnalyticsSucceeded(
            "update_billing_address",
            analyticsPayloadField("invocation_source", "payment_element"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )

        withContext(Dispatchers.Main) {
            controller.selectSavedPaymentMethod(PaymentSelection.Saved(createPaymentMethodWithBillingAddress()))
                .getOrThrow()
        }
        markTestSucceeded()
    }

    @Test
    fun testConfigureReportsInitStartedAndSucceededWithAttemptedConfiguration() =
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            renderPaymentElementContent = false,
            setup = { controller ->
                networkRule.validateCheckoutInitStartedAndSucceeded(
                    analyticsPayloadField("merchant_display_name", "true"),
                )
                assertThat(
                    controller.configure(
                        DEFAULT_CLIENT_SECRET,
                        CheckoutElementsController.Configuration()
                            .merchantDisplayName("Example Merchant")
                            .paymentElement(PaymentElement.Configuration()),
                    )
                ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
            },
        ) {
            markTestSucceeded()
        }

    @Test
    fun testUpdateEmailReportsSucceededWithoutDurationOrConfiguration() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "checkout_elements.update_email.succeeded",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            not(hasQueryParam("duration")),
            not(hasQueryParam("error_type")),
            not(hasQueryParam("merchant_display_name")),
        )

        assertThat(controller.updateEmail("email@example.com"))
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testClearPaymentOptionReportsInvokedAndBillingAddressEvents() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "checkout_elements.clear_payment_option",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            not(hasQueryParam("duration")),
            not(hasQueryParam("error_type")),
            not(hasQueryParam("merchant_display_name")),
        )
        networkRule.validateCheckoutAnalyticsStarted(
            "update_billing_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "false"),
        )
        networkRule.validateCheckoutAnalyticsSucceeded(
            "update_billing_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "false"),
        )

        assertThat(controller.clearPaymentOption())
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testClearPaymentOptionWithBillingAutomaticTaxReportsTaxUpdate() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        checkoutInitResponse = ::billingTaxCheckoutInitResponse,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "checkout_elements.clear_payment_option",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
        )
        networkRule.validateCheckoutAnalyticsStarted(
            "update_billing_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        networkRule.checkoutUpdate(bodyPart("tax_region", "")) { response ->
            billingTaxCheckoutInitResponse(response)
        }
        networkRule.validateCheckoutAnalyticsSucceeded(
            "update_billing_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )

        assertThat(controller.clearPaymentOption())
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testRunServerUpdateReportsSucceeded() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateCheckoutAnalyticsStarted(
            "run_server_update",
            not(hasQueryParam("merchant_update_duration")),
            not(hasQueryParam("refresh_duration")),
        )
        networkRule.checkoutInitWithAnalytics(responseFactory = { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.remove("link_settings")
            }
        })
        networkRule.validateCheckoutAnalyticsSucceeded(
            "run_server_update",
            not(hasQueryParam("merchant_update_duration")),
            not(hasQueryParam("refresh_duration")),
        )

        assertThat(controller.runServerUpdate { Result.success(Unit) })
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testRunServerUpdateMerchantFailureReportsFailedWithoutRefresh() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateCheckoutAnalyticsStarted(
            "run_server_update",
            not(hasQueryParam("merchant_update_duration")),
            not(hasQueryParam("refresh_duration")),
        )
        networkRule.validateCheckoutAnalyticsFailed(
            "run_server_update",
            not(hasQueryParam("merchant_update_duration")),
            not(hasQueryParam("refresh_duration")),
            analyticsPayloadField("error_type", "java.lang.IllegalStateException"),
        )

        assertThat(controller.runServerUpdate { Result.failure(IllegalStateException("boom")) })
            .isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        markTestSucceeded()
    }

    @Test
    fun testUpdateShippingAddressWithoutAutomaticTaxReportsSucceeded() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateCheckoutAnalyticsStarted(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "false"),
        )
        networkRule.validateCheckoutAnalyticsSucceeded(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "false"),
        )

        assertThat(controller.updateShippingAddress(name = "Jane Doe", address = shippingAddress()))
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testClearAlreadyNullShippingAddressReportsSucceeded() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        checkoutInitResponse = ::shippingTaxCheckoutInitResponse,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "checkout_elements.update_shipping_address.succeeded",
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "false"),
            not(hasQueryParam("duration")),
            not(hasQueryParam("error_type")),
            not(hasQueryParam("merchant_display_name")),
        )

        assertThat(controller.updateShippingAddress(name = null, address = null))
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testUpdateShippingAddressWithShippingAutomaticTaxReportsTaxUpdate() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        checkoutInitResponse = ::shippingTaxCheckoutInitResponse,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateCheckoutAnalyticsStarted(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", "510 Townsend St"),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94103"),
        ) { response ->
            shippingTaxCheckoutInitResponse(response)
        }
        networkRule.validateCheckoutAnalyticsSucceeded(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )

        assertThat(controller.updateShippingAddress(name = "Jane Doe", address = shippingAddress()))
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testClearShippingAddressWithShippingAutomaticTaxReportsTaxUpdate() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        checkoutInitResponse = ::shippingTaxCheckoutInitResponse,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStartedAndSucceeded()
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.validateCheckoutAnalyticsStarted(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", "510 Townsend St"),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94103"),
        ) { response ->
            shippingTaxCheckoutInitResponse(response)
        }
        networkRule.validateCheckoutAnalyticsSucceeded(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        assertThat(controller.updateShippingAddress(name = "Jane Doe", address = shippingAddress()))
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)

        networkRule.validateCheckoutAnalyticsStarted(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        networkRule.checkoutUpdate(bodyPart("tax_region", "")) { response ->
            shippingTaxCheckoutInitResponse(response)
        }
        networkRule.validateCheckoutAnalyticsSucceeded(
            "update_shipping_address",
            analyticsPayloadField("invocation_source", "merchant"),
            analyticsPayloadField("did_request_tax_update", "true"),
        )
        assertThat(controller.updateShippingAddress(name = null, address = null))
            .isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)
        markTestSucceeded()
    }

    @Test
    fun testConfigureApiFailureReportsInitFailedAnalytics() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        checkoutInitResponse = ::checkoutInitApiFailureResponse,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutAnalyticsStarted("init", hasQueryParam("merchant_display_name"))
            networkRule.validateCheckoutAnalyticsFailed(
                "init",
                hasQueryParam("merchant_display_name"),
                analyticsPayloadField("error_type", "invalid_request_error"),
                analyticsPayloadField("error_code", "checkout_session_invalid"),
                analyticsPayloadField("request_id", INIT_FAILURE_REQUEST_ID),
            )
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutElementsController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutElementsController.OperationResult.Failed::class.java)
        },
    ) {
        markTestSucceeded()
    }

    private fun billingTaxCheckoutInitResponse(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.remove("link_settings")
            // Google Pay only returns a full billing address when billing collection is required.
            json.put("billing_address_collection", "required")
            json.put(
                "automatic_tax",
                JSONObject()
                    .put("enabled", true)
                    .put("status", "ready")
                    .put("address_source", "billing"),
            )
            json.getJSONObject("automatic_tax").put("status", "requires_address")
        }
    }

    private fun shippingTaxCheckoutInitResponse(response: MockResponse) {
        createCheckoutInitResponseWithRequiredShippingAddress(response) { json ->
            json.remove("link_settings")
            json.put(
                "automatic_tax",
                JSONObject()
                    .put("enabled", true)
                    .put("status", "requires_address")
                    .put("address_source", "shipping"),
            )
        }
    }

    private fun shippingAddress(): CheckoutElementsController.Address {
        return CheckoutElementsController.Address(country = "US")
            .line1("510 Townsend St")
            .city("San Francisco")
            .state("CA")
            .postalCode("94103")
    }

    private fun checkoutInitApiFailureResponse(response: MockResponse) {
        response.setResponseCode(400)
        response.setHeader("Request-Id", INIT_FAILURE_REQUEST_ID)
        response.setBody(
            """
            {"error":{"type":"invalid_request_error","code":"checkout_session_invalid","message":"Invalid session"}}
            """.trimIndent()
        )
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val UPDATED_TOTAL = 5399
        const val INIT_FAILURE_REQUEST_ID = "req_checkout_init_failed"
    }
}

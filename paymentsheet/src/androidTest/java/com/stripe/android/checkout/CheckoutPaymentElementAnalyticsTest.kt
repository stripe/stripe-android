package com.stripe.android.checkout

import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
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
            networkRule.validateCheckoutInitStarted(DEFAULT_CHECKOUT_SESSION_ID)
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateCheckoutInitSucceeded(
                DEFAULT_CHECKOUT_SESSION_ID,
                analyticsPayloadField("ordered_lpms", "card,cashapp")
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_initial_displayed_payment_methods",
                productUsage = setOf("checkout_elements"),
                analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            )
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
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

        networkRule.validateCheckoutInitStarted(DEFAULT_CHECKOUT_SESSION_ID)
        networkRule.validateCheckoutInitFailed(
            DEFAULT_CHECKOUT_SESSION_ID,
            analyticsPayloadField("error_type", "java.lang.IllegalStateException"),
        )
        assertThat(
            controller.configure(
                DEFAULT_CLIENT_SECRET,
                configurationWithDefaultBillingCountry(null),
            )
        ).isInstanceOf(CheckoutController.OperationResult.Failed::class.java)

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
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val failure = result as CheckoutController.Result.Failed
                assertThat(failure.error).isInstanceOf(LocalStripeException::class.java)
            },
            checkoutInitResponse = ::billingTaxCheckoutInitResponse,
            setup = { controller ->
                networkRule.validateCheckoutInitStarted(DEFAULT_CHECKOUT_SESSION_ID)
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_load_started",
                    productUsage = setOf("checkout_elements"),
                )
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_load_succeeded",
                    productUsage = setOf("checkout_elements"),
                )
                networkRule.validateCheckoutInitSucceeded(DEFAULT_CHECKOUT_SESSION_ID)
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_initial_displayed_payment_methods",
                    productUsage = setOf("checkout_elements"),
                    analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
                )
                assertThat(
                    controller.configure(
                        DEFAULT_CLIENT_SECRET,
                        CheckoutController.Configuration().paymentElement(PaymentElement.Configuration()),
                    )
                ).isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
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
            networkRule.checkoutInit(responseFactory = ::billingTaxCheckoutInitResponse)
            networkRule.validateConfirmationStartedAnalyticsRequest(
                invocationSource = "merchant",
                selectedLpm = "google_pay",
            )
            networkRule.validateConfirmationResultAnalyticsRequest(
                eventName = "checkout_elements.confirm.failed",
                invocationSource = "merchant",
                selectedLpm = "google_pay",
                analyticsPayloadField("error_type", "checkoutSessionTotalChanged"),
                analyticsPayloadField("error_code", "checkout_session_total_changed"),
            )
            // The Checkout Session is refreshed after confirmation fails.
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("checkout_elements"),
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
                networkRule.validateCheckoutInitStarted(DEFAULT_CHECKOUT_SESSION_ID)
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_load_started",
                    productUsage = setOf("checkout_elements"),
                )
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_load_succeeded",
                    productUsage = setOf("checkout_elements"),
                )
                networkRule.validateCheckoutInitSucceeded(DEFAULT_CHECKOUT_SESSION_ID)
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_initial_displayed_payment_methods",
                    productUsage = setOf("checkout_elements"),
                    analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
                )
                assertThat(
                    controller.configure(
                        DEFAULT_CLIENT_SECRET,
                        CheckoutController.Configuration().paymentElement(PaymentElement.Configuration()),
                    )
                ).isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
            },
        ) {
            contentPage.waitUntilVisible()
            networkRule.validateAnalyticsRequest(
                eventName = "unexpected_error.checkout.saved_payment_method.missing_billing_address",
                productUsage = setOf("checkout_elements"),
            )
            // Changing the selection reloads the payment element.
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
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
    fun testConfigureApiFailureSendsInitFailureAnalytics() = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        checkoutInitResponse = ::checkoutInitApiFailureResponse,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStarted(DEFAULT_CHECKOUT_SESSION_ID)
            networkRule.validateCheckoutInitFailed(
                sessionId = DEFAULT_CHECKOUT_SESSION_ID,
                analyticsPayloadField("error_type", "invalid_request_error"),
                analyticsPayloadField("error_code", "checkout_session_invalid"),
                analyticsPayloadField("request_id", INIT_FAILURE_REQUEST_ID),
            )
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutController.OperationResult.Failed::class.java)
        },
    ) {
        markTestSucceeded()
    }

    @Test
    fun testFailedReconfigureUsesFreshResponseMetadataForInitFailureAnalytics() =
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            renderPaymentElementContent = false,
            setup = { controller ->
                networkRule.validateCheckoutInitStarted(DEFAULT_CHECKOUT_SESSION_ID)
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_load_started",
                    productUsage = setOf("checkout_elements"),
                )
                networkRule.validateAnalyticsRequest(
                    eventName = "mc_load_succeeded",
                    productUsage = setOf("checkout_elements"),
                )
                networkRule.validateCheckoutInitSucceeded(DEFAULT_CHECKOUT_SESSION_ID)
                assertThat(
                    controller.configure(
                        DEFAULT_CLIENT_SECRET,
                        CheckoutController.Configuration().paymentElement(PaymentElement.Configuration()),
                    )
                ).isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
            },
        ) {
            networkRule.checkoutInit(sessionId = RECONFIGURED_SESSION_ID) { response ->
                billingTaxCheckoutInitResponse(
                    response = response,
                    sessionId = RECONFIGURED_SESSION_ID,
                    configId = RECONFIGURED_CONFIG_ID,
                )
            }
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "CA"),
                sessionId = RECONFIGURED_SESSION_ID,
            ) { response ->
                response.setResponseCode(400)
                response.setHeader("Request-Id", INIT_FAILURE_REQUEST_ID)
                response.setBody(
                    """
                    {"error":{"type":"invalid_request_error","code":"invalid_tax_region","message":"Invalid tax region"}}
                    """.trimIndent()
                )
            }
            networkRule.validateCheckoutInitStarted(RECONFIGURED_SESSION_ID)
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateCheckoutInitFailed(
                RECONFIGURED_SESSION_ID,
                analyticsPayloadField("checkout_config_id", RECONFIGURED_CONFIG_ID),
                analyticsPayloadField("automatic_tax_enabled", "true"),
                analyticsPayloadField("error_type", "invalid_request_error"),
                analyticsPayloadField("error_code", "invalid_tax_region"),
                analyticsPayloadField("request_id", INIT_FAILURE_REQUEST_ID),
            )
            assertThat(
                controller.configure(
                    clientSecret = "${RECONFIGURED_SESSION_ID}_secret_example",
                    configuration = configurationWithDefaultBillingCountry("CA"),
                )
            ).isInstanceOf(CheckoutController.OperationResult.Failed::class.java)
            assertThat(controller.session.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)

            markTestSucceeded()
        }

    @Test
    fun testMalformedClientSecretIsOmittedFromInitAnalytics() = testClientSecretIsOmittedFromInitAnalytics(
        clientSecret = "not_a_client_secret",
        sessionId = "not_a_client_secret",
    )

    @Test
    fun testEmptySessionIdIsOmittedFromInitAnalytics() = testClientSecretIsOmittedFromInitAnalytics(
        clientSecret = "_secret_example",
        sessionId = "",
    )

    private fun testClientSecretIsOmittedFromInitAnalytics(
        clientSecret: String,
        sessionId: String,
    ) = runCheckoutPaymentElementScenario(
        networkRule = networkRule,
        renderPaymentElementContent = false,
        setup = { controller ->
            networkRule.validateCheckoutInitStarted(DEFAULT_CHECKOUT_SESSION_ID)
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_started",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_load_succeeded",
                productUsage = setOf("checkout_elements"),
            )
            networkRule.validateCheckoutInitSucceeded(DEFAULT_CHECKOUT_SESSION_ID)
            assertThat(
                controller.configure(
                    DEFAULT_CLIENT_SECRET,
                    CheckoutController.Configuration().paymentElement(PaymentElement.Configuration()),
                )
            ).isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
        },
    ) {
        networkRule.checkoutInit(
            sessionId = sessionId,
            responseFactory = ::checkoutInitApiFailureResponse,
        )
        networkRule.validateCheckoutInitStarted(sessionId = null)
        networkRule.validateCheckoutInitFailed(
            sessionId = null,
            analyticsPayloadField("error_type", "invalid_request_error"),
            analyticsPayloadField("error_code", "checkout_session_invalid"),
            analyticsPayloadField("request_id", INIT_FAILURE_REQUEST_ID),
        )

        assertThat(controller.configure(clientSecret))
            .isInstanceOf(CheckoutController.OperationResult.Failed::class.java)

        markTestSucceeded()
    }

    private fun billingTaxCheckoutInitResponse(response: MockResponse) {
        billingTaxCheckoutInitResponse(
            response = response,
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            configId = null,
        )
    }

    private fun billingTaxCheckoutInitResponse(
        response: MockResponse,
        sessionId: String,
        configId: String?,
    ) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("id", sessionId)
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
            configId?.let {
                json.put("checkout_session_config_id", it)
            }
            json.getJSONObject("automatic_tax").put("status", "requires_address")
        }
    }

    private fun configurationWithDefaultBillingCountry(country: String?): CheckoutController.Configuration =
        CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration())
            .defaults(
                CheckoutController.Configuration.Defaults().billingDetails(
                    CheckoutController.Configuration.Defaults.ContactDetails().address(
                        CheckoutController.Address().apply { country?.let { country(it) } }
                    )
                )
            )

    private fun checkoutInitApiFailureResponse(response: MockResponse) {
        response.setResponseCode(400)
        response.setHeader("Request-Id", INIT_FAILURE_REQUEST_ID)
        response.setBody(
            """{"error":{"type":"invalid_request_error","code":"checkout_session_invalid","message":"Invalid session"}}"""
        )
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val UPDATED_TOTAL = 5399
        const val INIT_FAILURE_REQUEST_ID = "req_checkout_init_failed"
        const val RECONFIGURED_SESSION_ID = "cs_test_reconfigured"
        const val RECONFIGURED_CONFIG_ID = "cs_config_reconfigured"
    }
}

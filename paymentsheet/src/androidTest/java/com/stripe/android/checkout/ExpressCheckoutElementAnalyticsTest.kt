package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkouttesting.CheckoutInitResponseFactory
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import okhttp3.mockwebserver.MockResponse
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
        runExpressCheckoutElementAnalyticsTest {
            val paymentMethod = PaymentMethodFactory.card()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            validateCheckoutOperationStarted(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )

            validateCheckoutOperationTerminal(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        runExpressCheckoutElementAnalyticsTest {
            enqueueSuccessfulNativeLinkPayment()

            networkRule.checkoutInit()

            validateCheckoutOperationStarted(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("link", linkContext = "wallet"),
            )
            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(eventName = "link.popup.success")

            validateCheckoutOperationTerminal(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("link", linkContext = "wallet"),
            )

            enqueueLinkAccountLookups(linkEnabled = true)

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testFailedGooglePayPayment() {
        runExpressCheckoutElementAnalyticsTest {

            enqueueFailedGooglePayPayment(IllegalStateException("Google Pay failed"))

            networkRule.checkoutInit()
            validateCheckoutOperationStarted(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )

            validateCheckoutOperationTerminal(
                "confirm",
                terminalPhase = "failed",
                requestMatchers = confirmationPayloadMatchers(
                    selectedLpm = "google_pay",
                    errorType = "IllegalStateException",
                ),
            )

            enqueueLinkAccountLookups(linkEnabled = true)

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testFailedNativeLinkPayment() {
        runExpressCheckoutElementAnalyticsTest {
            enqueueFailedNativeLinkPayment(IllegalStateException("Link failed"))

            networkRule.checkoutInit()
            validateCheckoutOperationStarted(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("link", linkContext = "wallet"),
            )
            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(
                eventName = "link.popup.error",
                analyticsPayloadField("error_message", "unknown"),
            )

            validateCheckoutOperationTerminal(
                "confirm",
                terminalPhase = "failed",
                requestMatchers = confirmationPayloadMatchers(
                    selectedLpm = "link",
                    linkContext = "wallet",
                    errorType = "IllegalStateException",
                ),
            )

            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
                validateLinkAccountLookupAnalyticsRequest()
            }

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testGooglePayUpdatesAutomaticTaxForRequiredShippingAddress() {
        runExpressCheckoutElementAnalyticsTest(
            initialCheckoutSessionResponseFactory =
                ::createCheckoutInitResponseWithRequiredShippingAddressForAutomaticTax,
        ) {
            val paymentMethod = PaymentMethodFactory.card()
            val shippingInformation = createShippingInformation()

            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
                shippingInformation = shippingInformation,
                paymentDataUpdate = GooglePayPaymentDataUpdate(
                    callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                    shippingAddress = GooglePayPaymentDataUpdate.ShippingAddress(
                        administrativeArea = "California",
                        countryCode = "US",
                        locality = "San Francisco",
                        postalCode = "94103",
                        iso3166AdministrativeArea = "US-CA",
                    ),
                ),
            )
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
            ) { response ->
                createCheckoutInitResponseWithRequiredShippingAddressForAutomaticTax(response)
            }
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
                bodyPart("shipping[name]", "Jenny Rosen"),
                bodyPart("shipping[address][line1]", "510 Townsend St"),
                bodyPart("shipping[address][line2]", "Floor 3"),
                bodyPart("shipping[address][city]", "San Francisco"),
                bodyPart("shipping[address][state]", "CA"),
                bodyPart("shipping[address][postal_code]", "94103"),
                bodyPart("shipping[address][country]", "US"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }
            validateCheckoutOperationStarted(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )
            validateCheckoutOperationAnalytics("update_shipping_address")

            validateCheckoutOperationTerminal(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithShippingAddressParameters(
            GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = setOf("US", "CA"),
            )
        )
    }

    @Test
    fun testGooglePaySendsRequiredBillingAddressForAutomaticTax() {
        runExpressCheckoutElementAnalyticsTest(
            linkEnabled = false,
            initialCheckoutSessionResponseFactory =
                CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
        ) {
            val paymentMethod = createPaymentMethodWithBillingAddress()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[line1]", "510 Townsend St"),
                bodyPart("tax_region[line2]", "Floor 3"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
            ) { response ->
                CheckoutInitResponseFactory
                    .createWithRequiredBillingAddressForAutomaticTax(response)
            }
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }
            validateCheckoutOperationStarted(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )
            validateCheckoutOperationAnalytics("update_billing_address")

            validateCheckoutOperationTerminal(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testGooglePayFailsWhenAutomaticTaxUpdateChangesTotal() {
        runExpressCheckoutElementAnalyticsTest(
            linkEnabled = false,
            initialCheckoutSessionResponseFactory =
                CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
        ) {
            val paymentMethod = createPaymentMethodWithBillingAddress()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)
            networkRule.checkoutUpdate { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONArray("checkout_items").getJSONObject(0)
                        .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                        .put("total", 5399)
                }
            }
            networkRule.checkoutInit(
                responseFactory = CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            )
            validateCheckoutOperationStarted(
                "confirm",
                requestMatchers = confirmationPayloadMatchers("google_pay"),
            )
            validateCheckoutOperationAnalytics("update_billing_address")

            validateCheckoutOperationTerminal(
                "confirm",
                terminalPhase = "failed",
                requestMatchers = confirmationPayloadMatchers(
                    selectedLpm = "google_pay",
                    errorType = "LocalStripeException",
                    errorCode = "checkout_session_total_changed",
                ),
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    private fun runExpressCheckoutElementAnalyticsTest(
        linkEnabled: Boolean = true,
        initialCheckoutSessionResponseFactory: (MockResponse) -> Unit = CheckoutInitResponseFactory::create,
        block: suspend ExpressCheckoutElementScenario.() -> Unit,
    ) {
        validateCheckoutOperationStarted("init")
        enqueueLinkAccountLookups(linkEnabled = linkEnabled)
        validateCheckoutOperationTerminal("init")
        validateAnalyticsRequest(
            eventName = "elements.express_checkout_element.init",
            productUsage = setOf("Checkout"),
        )

        runExpressCheckoutElementScenario(
            networkRule = networkRule,
            initialCheckoutSessionResponseFactory = initialCheckoutSessionResponseFactory,
            resultCallback = {
                // We expect the result callback to be called but test the result in other tests.
            },
        ) {
            block()
        }
    }

    private fun enqueueLinkAccountLookups(linkEnabled: Boolean) {
        if (linkEnabled) {
            // We load twice, once for PE and once for ECE.
            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
                validateLinkAccountLookupAnalyticsRequest()
            }
        }
    }

    private fun validateCheckoutOperationAnalytics(
        operation: String,
        terminalPhase: String = "succeeded",
    ) {
        validateCheckoutOperationStarted(operation)
        validateCheckoutOperationTerminal(operation, terminalPhase)
    }

    private fun validateCheckoutOperationStarted(
        operation: String,
        requestMatchers: List<RequestMatcher> = emptyList(),
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "checkout_elements.$operation.started",
            productUsage = setOf("checkout_elements"),
            *requestMatchers.toTypedArray(),
        )
    }

    private fun validateCheckoutOperationTerminal(
        operation: String,
        terminalPhase: String = "succeeded",
        requestMatchers: List<RequestMatcher> = emptyList(),
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = "checkout_elements.$operation.$terminalPhase",
            productUsage = setOf("checkout_elements"),
            *requestMatchers.toTypedArray(),
        )
    }

    private fun confirmationPayloadMatchers(
        selectedLpm: String,
        linkContext: String? = null,
        errorType: String? = null,
        errorCode: String? = null,
    ): List<RequestMatcher> = buildList {
        add(analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID))
        add(analyticsPayloadField("selected_lpm", selectedLpm))
        add(analyticsPayloadField("invocation_source", "express_checkout_element"))
        linkContext?.let { add(analyticsPayloadField("link_context", it)) }
        errorType?.let { add(analyticsPayloadField("error_type", it)) }
        errorCode?.let { add(analyticsPayloadField("error_code", it)) }
    }

    private fun validateLinkAccountLookupAnalyticsRequest() {
        validateAnalyticsRequest(eventName = "link.account_lookup.complete")
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
        productUsage: Set<String> = setOf("Checkout"),
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = productUsage,
            *requestMatchers,
        )
    }
}

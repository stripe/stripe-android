package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkouttesting.CheckoutInitResponseFactory
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.not
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

            validateAnalyticsRequest(eventName = "stripe_android.payment_intent_retrieval")
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationSucceeded(
                selectedLpm = "google_pay",
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        runExpressCheckoutElementAnalyticsTest {
            enqueueSuccessfulNativeLinkPayment()

            networkRule.checkoutInitWithAnalytics()

            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(eventName = "link.popup.success")
            validateConfirmationStarted(
                selectedLpm = "link",
                analyticsPayloadField("link_context", "wallet"),
            )
            validateConfirmationSucceeded(
                selectedLpm = "link",
                analyticsPayloadField("link_context", "wallet"),
            )

            // Confirmation reloads Link state for both PE and ECE.
            enqueueLoadingRequests(linkEnabled = true)

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testFailedGooglePayPayment() {
        runExpressCheckoutElementAnalyticsTest {

            enqueueFailedGooglePayPayment(IllegalStateException("Google Pay failed"))

            networkRule.checkoutInitWithAnalytics()
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationFailed(
                selectedLpm = "google_pay",
                analyticsPayloadField("error_type", "googlePay_1"),
                analyticsPayloadField("error_code", "1"),
            )

            // Confirmation reloads Link state for both PE and ECE.
            enqueueLoadingRequests(linkEnabled = true)

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testCanceledGooglePayPayment() {
        runExpressCheckoutElementAnalyticsTest {
            enqueueCanceledGooglePayPayment()

            networkRule.checkoutInitWithAnalytics()
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationCanceled(
                selectedLpm = "google_pay",
            )

            // Confirmation reloads Link state for both PE and ECE.
            enqueueLoadingRequests(linkEnabled = true)

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testFailedNativeLinkPayment() {
        runExpressCheckoutElementAnalyticsTest {
            enqueueFailedNativeLinkPayment(IllegalStateException("Link failed"))

            networkRule.checkoutInitWithAnalytics()
            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(
                eventName = "link.popup.error",
                analyticsPayloadField("error_message", "unknown"),
            )
            validateConfirmationStarted(
                selectedLpm = "link",
                analyticsPayloadField("link_context", "wallet"),
            )
            validateConfirmationFailed(
                selectedLpm = "link",
                analyticsPayloadField("link_context", "wallet"),
                analyticsPayloadField("error_type", "java.lang.IllegalStateException"),
            )

            // Confirmation reloads Link state for both PE and ECE.
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
                    shippingAddress = GooglePayPaymentDataUpdate.GooglePayAddress(
                        administrativeArea = "California",
                        countryCode = "US",
                        locality = "San Francisco",
                        postalCode = "94103",
                        iso3166AdministrativeArea = "US-CA",
                    ),
                    billingAddress = null,
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
            networkRule.validateCheckoutAnalyticsStarted("update_shipping_address", *addressUpdateMatchers())
            networkRule.validateCheckoutAnalyticsSucceeded("update_shipping_address", *addressUpdateMatchers())
            validateAnalyticsRequest(eventName = "stripe_android.payment_intent_retrieval")
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
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationSucceeded(
                selectedLpm = "google_pay",
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
            networkRule.validateCheckoutAnalyticsStarted("update_billing_address", *addressUpdateMatchers())
            networkRule.validateCheckoutAnalyticsSucceeded("update_billing_address", *addressUpdateMatchers())
            validateAnalyticsRequest(eventName = "stripe_android.payment_intent_retrieval")
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationSucceeded(
                selectedLpm = "google_pay",
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
                    json.getJSONArray("items").getJSONObject(0)
                        .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                        .getJSONObject("amount_details")
                        .put("total", 5399)
                    json.getJSONObject("total_summary").put("total", 5399)
                }
            }
            networkRule.validateCheckoutAnalyticsStarted("update_billing_address", *addressUpdateMatchers())
            networkRule.validateCheckoutAnalyticsSucceeded("update_billing_address", *addressUpdateMatchers())
            networkRule.checkoutInitWithAnalytics(
                responseFactory = CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
            )
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationFailed(
                selectedLpm = "google_pay",
                analyticsPayloadField("error_type", "checkoutSessionTotalChanged"),
                analyticsPayloadField("error_code", "checkout_session_total_changed"),
            )

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testGooglePayPaymentMethodCallbackReportsBillingTaxUpdate() {
        runExpressCheckoutElementAnalyticsTest(
            linkEnabled = false,
            initialCheckoutSessionResponseFactory =
                CheckoutInitResponseFactory::createWithRequiredBillingAddressForAutomaticTax,
        ) {
            val paymentMethod = PaymentMethodFactory.card()
            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
                paymentDataUpdate = GooglePayPaymentDataUpdate(
                    callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod,
                    shippingAddress = null,
                    billingAddress = googlePayAddress(),
                ),
            )
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
            ) { response ->
                CheckoutInitResponseFactory.createWithRequiredBillingAddressForAutomaticTax(response)
            }
            networkRule.validateCheckoutAnalyticsStarted("update_billing_address", *addressUpdateMatchers())
            networkRule.validateCheckoutAnalyticsSucceeded("update_billing_address", *addressUpdateMatchers())
            validateAnalyticsRequest(eventName = "stripe_android.payment_intent_retrieval")
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationSucceeded(selectedLpm = "google_pay")

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    @Test
    fun testGooglePayShippingAddressCallbackReportsLocalSuccess() {
        runExpressCheckoutElementAnalyticsTest {
            val paymentMethod = PaymentMethodFactory.card()
            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
                paymentDataUpdate = GooglePayPaymentDataUpdate(
                    callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                    shippingAddress = googlePayAddress(),
                    billingAddress = null,
                ),
            )
            networkRule.validateCheckoutSessionAnalyticsRequest(
                "checkout_elements.update_shipping_address.succeeded",
                analyticsPayloadField("invocation_source", "express_checkout_element"),
                analyticsPayloadField("did_request_tax_update", "false"),
                not(hasQueryParam("duration")),
            )
            validateAnalyticsRequest(eventName = "stripe_android.payment_intent_retrieval")
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationSucceeded(selectedLpm = "google_pay")

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testGooglePayShippingAddressCallbackReportsTaxUpdateFailure() {
        runExpressCheckoutElementAnalyticsTest(
            initialCheckoutSessionResponseFactory =
                ::createCheckoutInitResponseWithRequiredShippingAddressForAutomaticTax,
        ) {
            enqueueCanceledGooglePayPayment(
                paymentDataUpdate = GooglePayPaymentDataUpdate(
                    callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                    shippingAddress = googlePayAddress(),
                    billingAddress = null,
                ),
                expectedPaymentDataUpdateError = GooglePayPaymentDataError(
                    reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                    message = "Something went wrong",
                    intent = GooglePayPaymentDataError.Intent.ShippingAddress,
                ),
            )
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
            ) { response ->
                response.setResponseCode(400)
                response.setBody(
                    """{"error":{"type":"invalid_request_error","code":"invalid_tax_region","message":"Invalid address"}}"""
                )
            }
            networkRule.validateCheckoutAnalyticsStarted("update_shipping_address", *addressUpdateMatchers())
            networkRule.validateCheckoutAnalyticsFailed(
                "update_shipping_address",
                *addressUpdateMatchers(),
                analyticsPayloadField("error_type", "invalid_request_error"),
                analyticsPayloadField("error_code", "invalid_tax_region"),
            )
            networkRule.checkoutInitWithAnalytics(
                responseFactory = ::createCheckoutInitResponseWithRequiredShippingAddressForAutomaticTax,
            )
            validateConfirmationStarted(selectedLpm = "google_pay")
            validateConfirmationCanceled(selectedLpm = "google_pay")
            enqueueLoadingRequests(linkEnabled = true)

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    private fun googlePayAddress() = GooglePayPaymentDataUpdate.GooglePayAddress(
        administrativeArea = "California",
        countryCode = "US",
        locality = "San Francisco",
        postalCode = "94103",
        iso3166AdministrativeArea = "US-CA",
    )

    private fun runExpressCheckoutElementAnalyticsTest(
        linkEnabled: Boolean = true,
        initialCheckoutSessionResponseFactory: (MockResponse) -> Unit = CheckoutInitResponseFactory::create,
        block: suspend ExpressCheckoutElementScenario.() -> Unit,
    ) {
        networkRule.validateCheckoutInitStartedAndSucceeded()
        enqueueLoadingRequests(linkEnabled = linkEnabled)
        networkRule.validateAnalyticsRequest(
            eventName = "elements.express_checkout_element.init",
            productUsage = setOf("checkout_elements"),
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

    private fun enqueueLoadingRequests(linkEnabled: Boolean) {
        if (linkEnabled) {
            // PE and ECE each load Link state.
            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
                validateLinkAccountLookupAnalyticsRequest()
            }
        }
    }

    private fun validateConfirmationStarted(
        selectedLpm: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateCheckoutAnalyticsStarted(
            "confirm",
            *confirmationMatchers(selectedLpm),
            *requestMatchers,
        )
    }

    private fun validateConfirmationSucceeded(
        selectedLpm: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateCheckoutAnalyticsSucceeded(
            "confirm",
            *confirmationMatchers(selectedLpm),
            *requestMatchers,
        )
    }

    private fun validateConfirmationFailed(
        selectedLpm: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateCheckoutAnalyticsFailed(
            "confirm",
            *confirmationMatchers(selectedLpm),
            *requestMatchers,
        )
    }

    private fun validateConfirmationCanceled(
        selectedLpm: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateCheckoutAnalyticsCanceled(
            "confirm",
            *confirmationMatchers(selectedLpm),
            *requestMatchers,
        )
    }

    private fun confirmationMatchers(selectedLpm: String) = arrayOf(
        analyticsPayloadField("invocation_source", "express_checkout_element"),
        analyticsPayloadField("selected_lpm", selectedLpm),
    )

    private fun addressUpdateMatchers() = arrayOf(
        analyticsPayloadField("invocation_source", "express_checkout_element"),
        analyticsPayloadField("did_request_tax_update", "true"),
    )

    private fun validateLinkAccountLookupAnalyticsRequest() {
        validateAnalyticsRequest(eventName = "link.account_lookup.complete")
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("checkout_elements"),
            *requestMatchers,
        )
    }
}

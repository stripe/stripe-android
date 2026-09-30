package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkouttesting.CheckoutInitResponseFactory
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

            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_success",
                analyticsPayloadField("selected_lpm", "google_pay"),
            )

            // We re-load after confirm succeeds, which triggers another two loads: one for ECE, one for PE.
            repeat(2) {
                validateLoadingAnalyticsRequests()
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        runExpressCheckoutElementAnalyticsTest {
            enqueueSuccessfulNativeLinkPayment()

            networkRule.checkoutInit()

            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(eventName = "link.popup.success")
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_success",
                analyticsPayloadField("selected_lpm", "link"),
                analyticsPayloadField("link_context", "wallet"),
            )

            // We re-load after confirm succeeds, which triggers another two loads: one for ECE, one for PE.
            // We also re-load Link state since the confirmation was for a Link payment method.
            enqueueLoadingRequests(linkEnabled = true)

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testFailedGooglePayPayment() {
        runExpressCheckoutElementAnalyticsTest {

            enqueueFailedGooglePayPayment(IllegalStateException("Google Pay failed"))

            networkRule.checkoutInit()
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_failure",
                analyticsPayloadField("selected_lpm", "google_pay"),
                analyticsPayloadField("error_message", "googlePay_1"),
                analyticsPayloadField("error_code", "1"),
            )

            // We re-load after confirmation fails, which triggers another two loads: one for ECE, one for PE.
            enqueueLoadingRequests(linkEnabled = true)

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testFailedNativeLinkPayment() {
        runExpressCheckoutElementAnalyticsTest {
            enqueueFailedNativeLinkPayment(IllegalStateException("Link failed"))

            networkRule.checkoutInit()
            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(
                eventName = "link.popup.error",
                analyticsPayloadField("error_message", "unknown"),
            )
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_failure",
                analyticsPayloadField("selected_lpm", "link"),
                analyticsPayloadField("link_context", "wallet"),
                analyticsPayloadField("error_message", "java.lang.IllegalStateException"),
            )

            // We re-load after confirmation fails, which triggers another two loads: one for ECE, one for PE.
            // We also re-load Link state since the confirmation was for a Link payment method.
            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
                validateLoadingAnalyticsRequests()
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
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_success",
                analyticsPayloadField("selected_lpm", "google_pay"),
            )

            // We re-load after confirm succeeds, which triggers another two loads: one for ECE, one for PE.
            repeat(2) {
                validateLoadingAnalyticsRequests()
            }

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
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_success",
                analyticsPayloadField("selected_lpm", "google_pay"),
            )

            // We re-load after confirm succeeds, which triggers another two loads: one for ECE, one for PE.
            repeat(2) {
                validateLoadingAnalyticsRequests()
            }

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
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_failure",
                analyticsPayloadField("selected_lpm", "google_pay"),
                analyticsPayloadField("error_message", "checkoutSessionTotalChanged"),
                analyticsPayloadField("error_code", "checkout_session_total_changed"),
            )

            // We re-load after confirmation fails, which triggers another two loads: one for ECE, one for PE.
            repeat(2) {
                validateLoadingAnalyticsRequests()
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalledWithRequiredBillingAddress()
    }

    private fun runExpressCheckoutElementAnalyticsTest(
        linkEnabled: Boolean = true,
        initialCheckoutSessionResponseFactory: (MockResponse) -> Unit = CheckoutInitResponseFactory::create,
        block: () -> Unit,
    ) {
        enqueueLoadingRequests(linkEnabled = linkEnabled)
        validateAnalyticsRequest(eventName = "elements.express_checkout_element.init")

        runExpressCheckoutElementTest(
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
        // We load twice, once for PE and once for ECE. So all these requests are made twice.
        repeat(2) {
            if (linkEnabled) {
                networkRule.enqueueLinkAccountLookup()
                validateLinkAccountLookupAnalyticsRequest()
            }
            validateLoadingAnalyticsRequests()
        }
    }

    private fun validateLoadingAnalyticsRequests() {
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
    }

    private fun validateLinkAccountLookupAnalyticsRequest() {
        validateAnalyticsRequest(eventName = "link.account_lookup.complete")
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
}

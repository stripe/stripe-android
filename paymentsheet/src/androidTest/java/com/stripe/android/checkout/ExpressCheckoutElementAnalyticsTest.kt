package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.FeatureFlags
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
        // We load twice, once for PE and once for ECE. So all these requests are made twice.
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
            validateLoadingAnalyticsRequests()
            validateLinkAccountLookupAnalyticsRequest()
        }

        validateAnalyticsRequest(eventName = "mc_ece_init")

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = {
                // We expect the result callback to be called but test the result in other tests.
            },
        ) {
            val paymentMethod = PaymentMethodFactory.card()

            enqueueSuccessfulGooglePayPayment(paymentMethod = paymentMethod)

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            validateAnalyticsRequest(
                eventName = "mc_ece_wallet_tapped",
                analyticsPayloadField("selected_lpm", "google_pay"),
            )
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
        // We load twice, once for PE and once for ECE. So all these requests are made twice.
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
            validateLoadingAnalyticsRequests()
            validateLinkAccountLookupAnalyticsRequest()
        }

        validateAnalyticsRequest(eventName = "mc_ece_init")

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = {
                // We expect the result callback to be called but test the result in other tests.
            },
        ) {
            enqueueSuccessfulNativeLinkPayment()

            networkRule.checkoutInit(responseFactory = CheckoutInitResponseFactory::create)

            validateAnalyticsRequest(eventName = "link.popup.show")
            validateAnalyticsRequest(
                eventName = "mc_ece_wallet_tapped",
                analyticsPayloadField("selected_lpm", "link"),
                analyticsPayloadField("link_context", "wallet"),
            )
            validateAnalyticsRequest(eventName = "link.popup.success")
            validateAnalyticsRequest(
                eventName = "mc_embedded_payment_success",
                analyticsPayloadField("selected_lpm", "link"),
                analyticsPayloadField("link_context", "wallet"),
            )

            // We re-load after confirm succeeds, which triggers another two loads: one for ECE, one for PE.
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

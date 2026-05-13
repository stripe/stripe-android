package com.stripe.android.checkout

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    @Test
    fun `CurrencySelectorContent fires currency_selector_init on display`() {
        validateAnalyticsRequest("fraud_detection_data_repository.api_failure")
        validateAnalyticsRequest("elements.adaptive_pricing.currency_selector_init")

        val checkout = createCheckout()

        composeRule.setContent {
            checkout.CurrencySelectorContent()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `updateCurrency fires currency_toggled on success`() {
        validateAnalyticsRequest("fraud_detection_data_repository.api_failure")
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-adaptive-pricing-integration-currency.json")
        }
        validateAnalyticsRequest("elements.adaptive_pricing.currency_toggled")

        val checkout = createCheckout()

        runBlocking {
            checkout.updateCurrency("usd").getOrThrow()
        }
    }

    @Test
    fun `updateCurrency fires currency_toggled_failed on failure`() {
        validateAnalyticsRequest("fraud_detection_data_repository.api_failure")
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }
        validateAnalyticsRequest("elements.adaptive_pricing.currency_toggled.failed")

        val checkout = createCheckout()

        runBlocking {
            val result = checkout.updateCurrency("invalid")
            assert(result.isFailure)
        }
    }

    private fun createCheckout(): Checkout {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            adaptivePricingInfo = DEFAULT_ADAPTIVE_PRICING_INFO,
        )
        val state = CheckoutStateFactory.create(
            checkoutSessionResponse = checkoutSessionResponse,
        )
        return Checkout.createWithState(applicationContext, state)
    }

    private fun validateAnalyticsRequest(eventName: String) {
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            query("event", eventName),
        ) { response ->
            response.status = "HTTP/1.1 200 OK"
        }
    }

    companion object {
        private val DEFAULT_ADAPTIVE_PRICING_INFO = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "eur",
            integrationAmount = 5099,
            integrationCurrency = "usd",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 4594,
                    conversionMarkupBps = 400,
                    currency = "eur",
                    presentmentExchangeRate = "0.900961",
                ),
            ),
        )
    }
}

package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseJsonParser
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutControllerAnalyticsTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    // Destroys built controllers when the test finishes. Outermost so it runs after NetworkRule has
    // validated the fire-and-forget analytics dispatched on the (now-cancelled) viewModelScope.
    private val destroyControllerRule = CleanupTestRule(CheckoutController::destroy)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(destroyControllerRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        // Fraud detection otherwise retries its (unmocked) data fetch repeatedly, spamming the tracked
        // analytics host with a variable number of api_failure events. Disabling it pins that to one.
        .around(AdvancedFraudSignalsTestRule())

    @Test
    fun `updateCurrency fires currency_toggled on success`() {
        validateAnalyticsRequest("fraud_detection_data_repository.api_failure")
        networkRule.checkoutUpdate(responseFactory = successResponseFactory())
        // A successful mutation reloads the payment element, which fires its own load analytics. These
        // are incidental to what this test asserts, but must be enqueued because the tracked host
        // fails the test on any unmocked request.
        enqueuePaymentElementReloadAnalytics()
        validateAnalyticsRequest("elements.adaptive_pricing.currency_toggled")

        val controller = createConfiguredController()

        runBlocking {
            controller.updateCurrency("usd").getOrThrow()
        }
    }

    @Test
    fun `updateCurrency fires currency_toggled_failed on failure`() {
        validateAnalyticsRequest("fraud_detection_data_repository.api_failure")
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }
        // The mutation fails before the payment element reloads, so no load analytics fire here.
        validateAnalyticsRequest("elements.adaptive_pricing.currency_toggled.failed")

        val controller = createConfiguredController()

        runBlocking {
            val result = controller.updateCurrency("invalid")
            assertThat(result.isFailure).isTrue()
        }
    }

    // Builds a controller whose committed state was restored (not fetched via configure), so no
    // configure-time analytics fire. The restored session is fully loadable so a successful mutation
    // can reload the payment element.
    private fun createConfiguredController(): CheckoutController {
        val response = CheckoutSessionResponseJsonParser.parse(adaptivePricingSessionJson())!!
        // A committed state as CheckoutStateLoader would produce it. The resolved metadata/
        // configuration are placeholders; the reload triggered by a successful mutation recomputes
        // and overwrites them from the response.
        val state = CheckoutControllerState(
            key = response.id,
            configuration = CheckoutController.Configuration().build(),
            checkoutSessionResponse = response,
            flagImages = null,
            collectedDetails = CheckoutCollectedDetails(),
            integrationLaunched = false,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            paymentSelection = null,
        )
        val savedStateHandle = SavedStateHandle().apply {
            set(CheckoutControllerStateHolder.STATE_KEY, state)
        }
        return destroyControllerRule.track(
            CheckoutController.Builder(
                application = applicationContext,
                savedStateHandle = savedStateHandle,
            ).build()
        )
    }

    private fun adaptivePricingSessionJson(): JSONObject {
        val bodyString = javaClass.classLoader!!
            .getResourceAsStream(ADAPTIVE_PRICING_FIXTURE)!!
            .reader().buffered().readText()
        return JSONObject(bodyString).apply {
            // The loader requires a billing email; Link is disabled so it doesn't fire an unrelated
            // consumer session lookup.
            put("customer_email", "checkout@example.com")
            getJSONObject("elements_session").remove("link_settings")
        }
    }

    private fun successResponseFactory(): (MockResponse) -> Unit = { response ->
        response.testBodyFromFile(ADAPTIVE_PRICING_FIXTURE) { json ->
            json.put("customer_email", "checkout@example.com")
            json.getJSONObject("elements_session").remove("link_settings")
        }
    }

    private fun enqueuePaymentElementReloadAnalytics() {
        validateAnalyticsRequest("mc_load_started")
        validateAnalyticsRequest("mc_load_succeeded")
        validateAnalyticsRequest("elements.google_pay_repository.is_ready_request_api_call_failure")
        validateAnalyticsRequest("google_pay.skipped_during_load")
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

    private companion object {
        const val ADAPTIVE_PRICING_FIXTURE = "checkout-session-adaptive-pricing-default.json"
    }
}

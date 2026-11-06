package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutAnalyticsTestFixtures
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.utils.FakeFraudDetectionDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class CheckoutSessionRepositoryAnalyticsTest {

    @get:Rule
    val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    private val checkoutSessionResponse = CheckoutAnalyticsTestFixtures.sessionResponse(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
    )

    @Test
    fun `init reports started and succeeded with the configuration params`() = runScenario {
        expectEvent(
            "init.started",
            doesNotContainValue(CLIENT_SECRET),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            analyticsPayloadField("merchant_display_name", "true"),
            analyticsPayloadField("defaults[email]", "true"),
            analyticsPayloadField("defaults[billing_details][name]", "false"),
            not(hasQueryParam("payment_element")),
            not(hasQueryParam("checkout_config_id")),
            not(hasQueryParam("ordered_lpms")),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "init.succeeded",
            doesNotContainValue(CLIENT_SECRET),
            *INIT_RESPONSE_SESSION_PARAMS.asPayloadMatchers(),
            analyticsPayloadField("merchant_display_name", "true"),
            analyticsPayloadField("defaults[email]", "true"),
            analyticsPayloadField("defaults[billing_details][name]", "false"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.init(
            clientSecret = CheckoutSessionClientSecret(CLIENT_SECRET),
            adaptivePricingAllowed = false,
            analyticsParams = CONFIGURATION_PARAMS,
            reportInitAnalytics = true,
        ).getOrThrow()
    }

    @Test
    fun `init reports started and failed with the error`() = runScenario {
        expectEvent(
            "init.started",
            doesNotContainValue(CLIENT_SECRET),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            analyticsPayloadField("merchant_display_name", "true"),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "init.failed",
            doesNotContainValue(CLIENT_SECRET),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            analyticsPayloadField("merchant_display_name", "true"),
            analyticsPayloadField("error_type", "invalid_request_error"),
            analyticsPayloadField("error_code", "checkout_session_invalid"),
            analyticsPayloadField("request_id", "req_123"),
            not(hasQueryParam("ordered_lpms")),
            hasQueryParam("duration"),
        )
        networkRule.checkoutInit { response ->
            response.setResponseCode(400)
            response.addHeader("Request-Id", "req_123")
            response.setBody(
                """
                    {
                        "error": {
                            "type": "invalid_request_error",
                            "code": "checkout_session_invalid",
                            "message": "Invalid session"
                        }
                    }
                """.trimIndent()
            )
        }

        val result = repository.init(
            clientSecret = CheckoutSessionClientSecret(CLIENT_SECRET),
            adaptivePricingAllowed = false,
            analyticsParams = mapOf("merchant_display_name" to true),
            reportInitAnalytics = true,
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `init succeeds without reporting analytics when disabled`() = runScenario {
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.init(
            clientSecret = CheckoutSessionClientSecret(CLIENT_SECRET),
            adaptivePricingAllowed = false,
            analyticsParams = CONFIGURATION_PARAMS,
            reportInitAnalytics = false,
        )

        assertThat(result.getOrThrow().clientSecret.value).isEqualTo(CLIENT_SECRET)
    }

    @Test
    fun `init fails without reporting analytics when disabled`() = runScenario {
        networkRule.checkoutInit { response ->
            response.setResponseCode(400)
            response.setBody(
                """
                    {
                        "error": {
                            "type": "invalid_request_error",
                            "message": "Invalid session"
                        }
                    }
                """.trimIndent()
            )
        }

        val result = repository.init(
            clientSecret = CheckoutSessionClientSecret(CLIENT_SECRET),
            adaptivePricingAllowed = false,
            analyticsParams = CONFIGURATION_PARAMS,
            reportInitAnalytics = false,
        )

        assertThat(result.exceptionOrNull()).isInstanceOf(APIException::class.java)
    }

    @Test
    fun `applyPromotionCode reports started and succeeded with the session params`() = runScenario {
        expectEvent(
            "apply_promotion_code.started",
            *CheckoutAnalyticsTestFixtures.sessionParams.asPayloadMatchers(),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "apply_promotion_code.succeeded",
            *INIT_RESPONSE_SESSION_PARAMS.asPayloadMatchers(),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.applyPromotionCode(
            checkoutSessionResponse = checkoutSessionResponse,
            promotionCode = "10OFF",
        ).getOrThrow()
    }

    @Test
    fun `applyPromotionCode reports started and failed with the error`() = runScenario {
        expectEvent(
            "apply_promotion_code.started",
            *CheckoutAnalyticsTestFixtures.sessionParams.asPayloadMatchers(),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "apply_promotion_code.failed",
            *CheckoutAnalyticsTestFixtures.sessionParams.asPayloadMatchers(),
            analyticsPayloadField("error_type", "invalid_request_error"),
            analyticsPayloadField("error_code", "promotion_code_invalid"),
            analyticsPayloadField("request_id", "req_123"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.addHeader("Request-Id", "req_123")
            response.setBody(
                """
                    {
                        "error": {
                            "type": "invalid_request_error",
                            "code": "promotion_code_invalid",
                            "message": "Invalid promotion code"
                        }
                    }
                """.trimIndent()
            )
        }

        val result = repository.applyPromotionCode(
            checkoutSessionResponse = checkoutSessionResponse,
            promotionCode = "INVALID",
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `removePromotionCode reports started and succeeded`() = runScenario {
        expectEvent(
            "remove_promotion_code.started",
            *CheckoutAnalyticsTestFixtures.sessionParams.asPayloadMatchers(),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "remove_promotion_code.succeeded",
            *INIT_RESPONSE_SESSION_PARAMS.asPayloadMatchers(),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.removePromotionCode(
            checkoutSessionResponse = checkoutSessionResponse,
        ).getOrThrow()
    }

    @Test
    fun `updateCurrency reports the requested currency on started and succeeded`() = runScenario {
        expectEvent(
            "update_currency.started",
            *CheckoutAnalyticsTestFixtures.sessionParams.asPayloadMatchers(),
            analyticsPayloadField("requested_currency", "eur"),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "update_currency.succeeded",
            *INIT_RESPONSE_SESSION_PARAMS.asPayloadMatchers(),
            analyticsPayloadField("requested_currency", "eur"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.updateCurrency(
            checkoutSessionResponse = checkoutSessionResponse,
            currencyCode = "eur",
        ).getOrThrow()
    }

    @Test
    fun `updateCurrency reports the requested currency and error type on started and failed`() = runScenario {
        expectEvent(
            "update_currency.started",
            *CheckoutAnalyticsTestFixtures.sessionParams.asPayloadMatchers(),
            analyticsPayloadField("requested_currency", "invalid"),
            not(hasQueryParam("duration")),
        )
        expectEvent(
            "update_currency.failed",
            *CheckoutAnalyticsTestFixtures.sessionParams.asPayloadMatchers(),
            analyticsPayloadField("requested_currency", "invalid"),
            analyticsPayloadField("error_type", "invalid_request_error"),
            hasQueryParam("duration"),
        )
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"type": "invalid_request_error", "message": "Invalid currency"}}""")
        }

        val result = repository.updateCurrency(
            checkoutSessionResponse = checkoutSessionResponse,
            currencyCode = "invalid",
        )

        assertThat(result.isFailure).isTrue()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val repository = CheckoutSessionRepository(
            stripeRepository = CheckoutStripeRepositoryFactory.create(),
            elementsSessionClientParams = CheckoutStripeRepositoryFactory.clientParams(),
            fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
            stripeNetworkClient = DefaultStripeNetworkClient(),
            analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
                stripeNetworkClient = DefaultStripeNetworkClient(workContext = coroutineContext + Dispatchers.IO),
                workContext = coroutineContext + Dispatchers.IO,
                logger = Logger.noop(),
            ),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = DEFAULT_API_CONFIG.publishableKey,
                defaultProductUsageTokens = setOf("checkout_elements"),
            ),
            apiRequestOptionsProvider = {
                ApiRequest.Options(
                    apiKey = DEFAULT_API_CONFIG.publishableKey,
                    stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
                )
            },
        )

        Scenario(repository).block()
    }

    private class Scenario(val repository: CheckoutSessionRepository)

    private fun expectEvent(operationAndOutcome: String, vararg matchers: RequestMatcher) {
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            analyticsPayloadField("event", "checkout_elements.$operationAndOutcome"),
            analyticsPayloadField("publishable_key", DEFAULT_API_CONFIG.publishableKey),
            analyticsPayloadField("product_usage", "checkout_elements"),
            *matchers,
            applyDefaultAuthorization = false,
        ) { response ->
            response.status = "HTTP/1.1 200 OK"
        }
    }

    private fun Map<String, Any>.asPayloadMatchers(): Array<RequestMatcher> =
        map { (key, value) -> analyticsPayloadField(key, value.toString()) }.toTypedArray()

    private fun doesNotContainValue(value: String) = RequestMatcher { request ->
        value !in request.queryParams.values
    }

    private companion object {
        const val CLIENT_SECRET = "cs_test_abc123_secret_test"

        // Nested maps are flattened into bracketed query keys, e.g. defaults[billing_details][name].
        val CONFIGURATION_PARAMS: Map<String, Any?> = mapOf(
            "merchant_display_name" to true,
            "defaults" to mapOf(
                "email" to true,
                "billing_details" to mapOf("name" to false),
            ),
            "payment_element" to null,
        )

        // Values come from checkout-session-init.json, the response parsed by the init and update requests.
        val INIT_RESPONSE_SESSION_PARAMS: Map<String, Any> = mapOf(
            "checkout_session_id" to "cs_test_abc123",
            "checkout_config_id" to "checkout_config_123",
            "currency" to "usd",
            "session_status" to "open",
            "payment_status" to "unpaid",
            "elements_session_config_id" to "cs_config_123",
            "adaptive_pricing_active" to false,
            "automatic_tax_enabled" to false,
            "ordered_lpms" to "card,link,apple_pay,google_pay,cashapp",
        )
    }
}

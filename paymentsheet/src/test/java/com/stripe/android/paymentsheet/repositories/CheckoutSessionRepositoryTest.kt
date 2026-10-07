package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CheckoutSessionRepositoryTest {

    @get:Rule
    val networkRule = NetworkRule()

    private val clientParams = ElementsSessionClientParams(
        mobileAppId = "com.stripe.android.paymentsheet.test",
        mobileSessionIdProvider = { AnalyticsRequestFactory.sessionId.toString() },
    )

    private val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()

    private val repository = createRepository(DefaultStripeNetworkClient())

    private fun createRepository(networkClient: StripeNetworkClient) = CheckoutSessionRepository(
        stripeNetworkClient = networkClient,
        analyticsRequestExecutor = analyticsRequestExecutor,
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_123",
        ),
        apiRequestOptionsProvider = {
            ApiRequest.Options(
                apiKey = DEFAULT_API_CONFIG.publishableKey,
                stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
            )
        },
    )

    @Test
    fun `poll request disables retries and bounds timeouts by the remaining budget`() = runTest {
        val calls = Turbine<StripeRequest>()
        val networkClient = object : StripeNetworkClient {
            override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
                calls.add(request)
                return StripeResponse(200, """{"session_id":"cs_test","state":"active"}""")
            }
            override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
                error("Unexpected file request")
            }
        }
        createRepository(networkClient).poll("cs_test", 1_500).getOrThrow()
        val request = calls.awaitItem()
        assertThat(request.retryResponseCodes).isEmpty()
        assertThat(request.connectTimeoutMillis).isEqualTo(750)
        assertThat(request.readTimeoutMillis).isEqualTo(750)
        calls.ensureAllEventsConsumed()
    }

    @Test
    fun `retrieve sends aggregation expected and returns the full session`() = runTest {
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID"),
            query("elements_session_client[is_aggregation_expected]", "true"),
        ) { it.testBodyFromFile("checkout-session-init.json") }

        val result = repository.retrieve(DEFAULT_CHECKOUT_SESSION_ID).getOrThrow()
        assertThat(result.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(result.checkoutItems).isNotEmpty()
    }

    @Test
    fun `poll returns the state from the poll endpoint`() = runTest {
        networkRule.enqueue(method("GET"), path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/poll")) {
            it.setBody("""{"session_id":"$DEFAULT_CHECKOUT_SESSION_ID","state":"succeeded"}""")
        }
        assertThat(repository.poll(DEFAULT_CHECKOUT_SESSION_ID, 1_000).getOrThrow().state)
            .isEqualTo(CheckoutSessionPollResponse.State.SUCCEEDED)
    }

    @Test
    fun `poll does not retry rate limited requests at the transport layer`() = runTest {
        networkRule.enqueue(method("GET"), path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/poll")) {
            it.setResponseCode(429)
            it.setBody("""{"error":{"message":"Rate limited"}}""")
        }
        val error = repository.poll(DEFAULT_CHECKOUT_SESSION_ID, 1_000).exceptionOrNull()
        assertThat((error as? com.stripe.android.core.exception.StripeException)?.statusCode).isEqualTo(429)
    }

    @Test
    fun `poll treats malformed responses as request failures`() = runTest {
        networkRule.enqueue(method("GET"), path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/poll")) {
            it.setBody("""{"state":"unsupported"}""")
        }
        assertThat(repository.poll(DEFAULT_CHECKOUT_SESSION_ID, 1_000).isFailure).isTrue()
    }

    @Test
    fun `init sends elements_session_client params`() = runTest {
        val expectedSessionId = AnalyticsRequestFactory.sessionId.toString()
        networkRule.checkoutInit(
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
            bodyPart("elements_session_client[mobile_session_id]", expectedSessionId),
            bodyPart("elements_session_client[mobile_app_id]", clientParams.mobileAppId),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.init(
            clientParams = clientParams,
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            adaptivePricingAllowed = true,
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `detach sends elements_session_client params`() = runTest {
        val expectedSessionId = AnalyticsRequestFactory.sessionId.toString()
        networkRule.checkoutUpdate(
            bodyPart("payment_method_to_detach", "pm_123"),
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
            bodyPart("elements_session_client[locale]", clientParams.locale),
            bodyPart("elements_session_client[mobile_session_id]", expectedSessionId),
            bodyPart("elements_session_client[mobile_app_id]", clientParams.mobileAppId),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.detachPaymentMethod(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            paymentMethodId = "pm_123",
            clientParams = clientParams,
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updateCurrency sends currency code and returns response on success`() = runTest {
        networkRule.checkoutUpdate(
            bodyPart("updated_currency", "eur"),
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "eur",
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updateCurrency returns failure on error response`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "invalid",
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `updateCurrency fires currency_toggled on success`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "eur",
        ).getOrThrow()

        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.adaptive_pricing.currency_toggled")
    }

    @Test
    fun `updateCurrency fires currency_toggled_failed on failure`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }

        val result = repository.updateCurrency(
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "invalid",
        )

        assertThat(result.isFailure).isTrue()
        val params = analyticsRequestExecutor.getExecutedRequests().single().params
        assertThat(params).containsEntry("event", "elements.adaptive_pricing.currency_toggled.failed")
    }
}

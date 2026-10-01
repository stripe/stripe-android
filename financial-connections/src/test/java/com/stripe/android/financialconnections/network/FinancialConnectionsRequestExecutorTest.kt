package com.stripe.android.financialconnections.network

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsResponseEventEmitter
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import java.util.Date
import java.util.Locale

internal class FinancialConnectionsRequestExecutorTest {

    @Test
    fun `background auth session telemetry preserves failure without emitting a public error`() = runScenario {
        val result = runCatching {
            repository.postAuthorizationSessionEvent(
                clientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                clientTimestamp = Date(0),
                sessionId = "fcauth_123",
                authSessionEvents = emptyList()
            )
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidRequestException::class.java)
        assertThat(networkClient.requests.awaitItem().url).endsWith("/events")
        publicEvents.expectNoEvents()
    }

    @Test
    fun `foreground cancellation still emits the backend public error`() = runScenario {
        val result = runCatching {
            repository.cancelAuthorizationSession(
                clientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                sessionId = "fcauth_123"
            )
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidRequestException::class.java)
        assertThat(networkClient.requests.awaitItem().url).endsWith("/cancel")
        val event = publicEvents.awaitItem()
        assertThat(event.name).isEqualTo(FinancialConnectionsEvent.Name.ERROR)
        assertThat(event.metadata.errorCode).isEqualTo(FinancialConnectionsEvent.ErrorCode.NO_ELIGIBLE_ACCOUNTS)
    }

    @Test
    fun `background request still decodes successful responses`() = runScenario(
        response = StripeResponse(code = 200, body = "\"recorded\"")
    ) {
        val request = ApiRequest.Factory().createPost(
            url = "https://api.stripe.com/v1/financial_connections/authorization_sessions/events",
            options = ApiRequest.Options(apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            params = emptyMap<String, Any>()
        )

        val result = requestExecutor.executeWithoutUserFacingEvents(request, String.serializer())

        assertThat(result).isEqualTo("recorded")
        assertThat(networkClient.requests.awaitItem()).isSameInstanceAs(request)
        publicEvents.expectNoEvents()
    }

    private fun runScenario(
        response: StripeResponse<String> = StripeResponse(
            code = 400,
            body = """
                {
                    "error": {
                        "message": "Unable to complete request",
                        "extra_fields": {
                            "events_to_emit": [{"type": "error", "error": {"error_code": "no_eligible_accounts"}}]
                        }
                    }
                }
            """.trimIndent()
        ),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val publicEvents = Turbine<FinancialConnectionsEvent>()
        val networkClient = FakeEventNetworkClient(response)
        val requestExecutor = FinancialConnectionsRequestExecutor(
            stripeNetworkClient = networkClient,
            eventEmitter = FinancialConnectionsResponseEventEmitter(Json.Default, Logger.noop()),
            json = Json.Default,
            logger = Logger.noop()
        )
        val repository = FinancialConnectionsManifestRepository(
            requestExecutor = requestExecutor,
            apiRequestFactory = ApiRequest.Factory(),
            provideApiRequestOptions = { ApiRequest.Options(apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            logger = Logger.noop(),
            locale = Locale.US,
            initialSync = null
        )
        FinancialConnections.setEventListener(publicEvents::add)
        try {
            Scenario(requestExecutor, repository, networkClient, publicEvents).block()
        } finally {
            FinancialConnections.clearEventListener()
            networkClient.requests.ensureAllEventsConsumed()
            publicEvents.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val requestExecutor: FinancialConnectionsRequestExecutor,
        val repository: FinancialConnectionsManifestRepository,
        val networkClient: FakeEventNetworkClient,
        val publicEvents: Turbine<FinancialConnectionsEvent>
    )
}

internal class FakeEventNetworkClient(
    var response: StripeResponse<String>
) : StripeNetworkClient {
    val requests = Turbine<StripeRequest>()

    override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
        requests.add(request)
        return response
    }

    override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
        error("File requests are not used by Financial Connections event tests")
    }
}

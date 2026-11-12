package com.stripe.android.paymentsheet.featuredinstitutions

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class DefaultFeaturedInstitutionsRepositoryTest {

    @Test
    fun `sends instant debits product, US country and limit of 12`() = runScenario {
        repository.retrieveFeaturedInstitutions(OPTIONS)

        val request = networkClient.requests.awaitItem() as ApiRequest
        assertThat(request.method).isEqualTo(StripeRequest.Method.GET)
        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/connections/featured_institutions")
        assertThat(request.params).isEqualTo(
            mapOf(
                "product" to "instant_debits",
                "country" to "US",
                "limit" to 12,
            )
        )
        assertThat(request.options).isEqualTo(OPTIONS)
    }

    @Test
    fun `parses institutions in order, tolerating missing and empty icons`() = runScenario(
        responseBody = """
            {
              "object": "list",
              "count": 3,
              "has_more": false,
              "data": [
                { "id": "chase", "name": "Chase", "icon": { "default": "https://example.com/chase.png" } },
                { "id": "bofa", "name": "Bank of America" },
                { "id": "wells", "name": "Wells Fargo", "icon": { "default": "" } },
                { "id": "capone", "name": "Capital One", "icon": {} }
              ]
            }
        """.trimIndent(),
    ) {
        val result = repository.retrieveFeaturedInstitutions(OPTIONS)

        networkClient.requests.awaitItem()
        assertThat(result.getOrThrow().data).containsExactly(
            FeaturedInstitution(id = "chase", name = "Chase", iconUrl = "https://example.com/chase.png"),
            FeaturedInstitution(id = "bofa", name = "Bank of America", iconUrl = null),
            FeaturedInstitution(id = "wells", name = "Wells Fargo", iconUrl = null),
            FeaturedInstitution(id = "capone", name = "Capital One", iconUrl = null),
        ).inOrder()
    }

    @Test
    fun `returns failure on error response`() = runScenario(responseCode = 500) {
        val result = repository.retrieveFeaturedInstitutions(OPTIONS)

        networkClient.requests.awaitItem()
        assertThat(result.isFailure).isTrue()
    }

    private fun runScenario(
        responseBody: String = """{ "data": [] }""",
        responseCode: Int = 200,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val networkClient = RecordingStripeNetworkClient(responseBody, responseCode)

        Scenario(
            networkClient = networkClient,
            repository = DefaultFeaturedInstitutionsRepository(networkClient),
        ).block()

        networkClient.requests.ensureAllEventsConsumed()
    }

    private class Scenario(
        val networkClient: RecordingStripeNetworkClient,
        val repository: DefaultFeaturedInstitutionsRepository,
    )

    private class RecordingStripeNetworkClient(
        private val responseBody: String,
        private val responseCode: Int,
    ) : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            return StripeResponse(code = responseCode, body = responseBody)
        }

        override suspend fun executeRequestForFile(
            request: StripeRequest,
            outputFile: File,
        ): StripeResponse<File> {
            requests.add(request)
            return StripeResponse(code = responseCode, body = outputFile)
        }
    }

    private companion object {
        val OPTIONS = ApiRequest.Options(apiKey = "pk_test_123", stripeAccount = "acct_123")
    }
}

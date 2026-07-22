package com.stripe.android.paymentsheet.addresselement

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
class StripeAutocompleteRepositoryTest {

    @Test
    fun `findAutocompletePredictions sends country_codes as list`() = runTest {
        val scenario = createScenario()

        scenario.repository.findAutocompletePredictions(
            query = "123 Main",
            country = "US",
            sessionToken = "session_abc",
            locale = null,
        )

        val request = scenario.networkClient.lastRequest as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["country_codes"]).isEqualTo(listOf("US"))
    }

    @Test
    fun `findAutocompletePredictions sends all params`() = runTest {
        val scenario = createScenario()

        scenario.repository.findAutocompletePredictions(
            query = "123 Main",
            country = "US",
            sessionToken = "session_abc",
            locale = "en",
        )

        val request = scenario.networkClient.lastRequest as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["search_text"]).isEqualTo("123 Main")
        assertThat(params["session_token"]).isEqualTo("session_abc")
        assertThat(params["client_type"]).isEqualTo("mobile")
        assertThat(params["locale"]).isEqualTo("en")
    }

    @Test
    fun `fetchPlaceDetails sends google source`() = runTest {
        val scenario = createScenario()

        scenario.repository.fetchPlaceDetails(
            placeId = "places/ChIJcznybKSAhYARlTOE-mb13JA",
            sessionToken = "session_123",
        )

        val request = scenario.networkClient.lastRequest as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["source"]).isEqualTo("google")
    }

    @Test
    fun `fetchPlaceDetails parses address correctly`() = runTest {
        val scenario = createScenario(
            responseBody = """
                {"address":{"line1":"123 Main St","city":"San Francisco",
                "state":"CA","postal_code":"94105","country":"US"}}
            """.trimIndent()
        )

        val result = scenario.repository.fetchPlaceDetails(
            placeId = "place_123",
            sessionToken = "session_123",
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().address).isEqualTo(
            StripeProxyAddress(
                line1 = "123 Main St",
                line2 = null,
                city = "San Francisco",
                state = "CA",
                postalCode = "94105",
                country = "US",
            )
        )
    }

    @Test
    fun `fetchPlaceDetails returns failure on error response`() = runTest {
        val scenario = createScenario(responseCode = 400)

        val result = scenario.repository.fetchPlaceDetails(
            placeId = "place_123",
            sessionToken = "session_123",
        )

        assertThat(result.isFailure).isTrue()
    }

    private fun createScenario(
        responseBody: String = DEFAULT_DETAILS_RESPONSE,
        responseCode: Int = 200
    ): Scenario {
        val networkClient = RecordingStripeNetworkClient(responseBody, responseCode)
        val repository = DefaultStripeAutocompleteRepository(
            stripeNetworkClient = networkClient,
            apiRequestFactory = ApiRequest.Factory(),
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
        )
        return Scenario(networkClient = networkClient, repository = repository)
    }

    private class Scenario(
        val networkClient: RecordingStripeNetworkClient,
        val repository: DefaultStripeAutocompleteRepository
    )

    private class RecordingStripeNetworkClient(
        private val responseBody: String,
        private val responseCode: Int
    ) : StripeNetworkClient {
        var lastRequest: StripeRequest? = null

        override suspend fun executeRequest(
            request: StripeRequest
        ): StripeResponse<String> {
            lastRequest = request
            return StripeResponse(code = responseCode, body = responseBody)
        }

        override suspend fun executeRequestForFile(
            request: StripeRequest,
            outputFile: File
        ): StripeResponse<File> {
            lastRequest = request
            return StripeResponse(code = responseCode, body = outputFile)
        }
    }

    private companion object {
        val DEFAULT_DETAILS_RESPONSE = """
            {"address":{"line1":"123 Main St","city":"San Francisco",
            "state":"CA","postal_code":"94105","country":"US"}}
        """.trimIndent()
    }
}

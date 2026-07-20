package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class StripeAutocompleteApiServiceTest {

    @Test
    fun `parseAutocompletePredictionsResponse uses display_data fields when primary text fields absent`() {
        val json = JSONObject(
            """
            {
              "source": "google",
              "suggestions": [
                {
                  "address": null,
                  "display_data": {
                    "matches": [
                      {
                        "end_offset": 3
                      }
                    ],
                    "subtitle": "San Francisco, CA, USA",
                    "title": "554 Fillmore Street"
                  },
                  "place_id": "places/ChIJcznybKSAhYARlTOE-mb13JA"
                }
              ]
            }
            """.trimIndent()
        )

        val result = AutocompletePredictionsResponseJsonParser.parse(json)

        assertThat(result?.predictions).hasSize(1)
        assertThat(result?.predictions?.single()).isEqualTo(
            AutocompleteSuggestion(
                placeId = "places/ChIJcznybKSAhYARlTOE-mb13JA",
                primaryText = "554 Fillmore Street",
                secondaryText = "San Francisco, CA, USA",
                address = null,
            )
        )
    }

    @Test
    fun `parseAutocompletePredictionsResponse prefers existing primary text fields`() {
        val json = JSONObject(
            """
            {
              "suggestions": [
                {
                  "address": null,
                  "display_data": {
                    "subtitle": "Ignored subtitle",
                    "title": "Ignored title"
                  },
                  "place_id": "place_123",
                  "primary_text": "123 Main St",
                  "secondary_text": "San Francisco, CA"
                }
              ]
            }
            """.trimIndent()
        )

        val result = AutocompletePredictionsResponseJsonParser.parse(json)

        assertThat(result?.predictions).hasSize(1)
        assertThat(result?.predictions?.single()).isEqualTo(
            AutocompleteSuggestion(
                placeId = "place_123",
                primaryText = "123 Main St",
                secondaryText = "San Francisco, CA",
                address = null,
            )
        )
    }

    @Test
    fun `fetchPlaceDetails sends google source`() = runTest {
        val networkClient = RecordingStripeNetworkClient()
        val service = DefaultStripeAutocompleteApiService(
            stripeNetworkClient = networkClient,
            apiRequestFactory = ApiRequest.Factory(),
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
        )

        service.fetchPlaceDetails(
            placeId = "places/ChIJcznybKSAhYARlTOE-mb13JA",
            sessionToken = "session_123",
        )

        val request = networkClient.lastRequest as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["source"]).isEqualTo("google")
    }

    private class RecordingStripeNetworkClient : StripeNetworkClient {
        var lastRequest: StripeRequest? = null

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            lastRequest = request
            return StripeResponse(
                code = 200,
                body = """
                    |{"address":{"line1":"123 Main St","city":"San Francisco",
                    |"state":"CA","postal_code":"94105","country":"US"}}
                """.trimMargin(),
            )
        }

        override suspend fun executeRequestForFile(
            request: StripeRequest,
            outputFile: File,
        ): StripeResponse<File> {
            lastRequest = request
            return StripeResponse(
                code = 200,
                body = outputFile,
            )
        }
    }
}

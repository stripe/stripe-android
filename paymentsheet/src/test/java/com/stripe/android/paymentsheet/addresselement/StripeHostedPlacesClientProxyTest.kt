package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.ui.core.elements.autocomplete.model.transformGoogleToStripeAddress
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class StripeHostedPlacesClientProxyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Suppress("LongMethod")
    @Test
    fun `findAutocompletePredictions sends expected request and measures duration`() = runTest {
        val stripeNetworkClient = mock<StripeNetworkClient>()
        val durationProvider = FakeDurationProvider()
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(
                code = 200,
                body = """
                    {
                      "suggestions": [
                        {
                          "display_data": {
                            "title": "354 Oyster Point Boulevard",
                            "subtitle": "South San Francisco, CA, USA"
                          },
                          "place_id": "places/ChIJr75gblx4j4AREm2k-W7EXcg"
                        }
                      ],
                      "source": "google"
                    }
                """.trimIndent(),
            )
        )

        val proxy = createProxy(
            stripeNetworkClient = stripeNetworkClient,
            durationProvider = durationProvider,
        )

        val result = proxy.findAutocompletePredictions(
            query = "354 Oys",
            country = "US",
            limit = 4,
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().autocompletePredictions).hasSize(1)
        assertThat(result.getOrThrow().autocompletePredictions.single().primaryText.toString())
            .isEqualTo("354 Oyster Point Boulevard")

        val requestCaptor = argumentCaptor<StripeRequest>()
        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val body = decodedBody(request)
        assertThat(request.url).isEqualTo("https://api.stripe.com/v1/elements/address/autocomplete")
        assertThat(body).contains("search_text=354 Oys")
        assertThat(body).contains("session_token=session-token")
        assertThat(body).contains("client_type=mobile")
        assertThat(body).contains("locale=en-US")
        assertThat(body).contains("country_codes[]=us")
        assertThat(body).contains("google_api_key=google-key")
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(
                    key = com.stripe.android.core.utils.DurationProvider.Key.AutocompleteFindPredictions,
                    reset = true,
                )
            )
        ).isTrue()
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.End(
                    key = com.stripe.android.core.utils.DurationProvider.Key.AutocompleteFindPredictions,
                )
            )
        ).isTrue()
    }

    @Test
    fun `fetchPlace uses cached address from autocomplete response without hitting details endpoint`() = runTest {
        val stripeNetworkClient = mock<StripeNetworkClient>()
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(
                code = 200,
                body = """
                    {
                      "suggestions": [
                        {
                          "display_data": {
                            "title": "354 Oyster Point Boulevard",
                            "subtitle": "South San Francisco, CA, USA"
                          },
                          "address": {
                            "line1": "354 Oyster Point Boulevard",
                            "line2": "",
                            "city": "South San Francisco",
                            "state": "CA",
                            "country": "US",
                            "postal_code": "94080-1912"
                          }
                        }
                      ],
                      "source": "google"
                    }
                """.trimIndent(),
            )
        )
        val proxy = createProxy(stripeNetworkClient = stripeNetworkClient)

        val prediction = proxy.findAutocompletePredictions(
            query = "354 Oys",
            country = "US",
            limit = 4,
        ).getOrThrow().autocompletePredictions.single()
        val place = proxy.fetchPlace(prediction.placeId).getOrThrow().place
        val address = place.transformGoogleToStripeAddress(Locale.US)

        assertThat(address.line1).isEqualTo("354 Oyster Point Boulevard")
        assertThat(address.city).isEqualTo("South San Francisco")
        assertThat(address.state).isEqualTo("CA")
        assertThat(address.country).isEqualTo("US")
        assertThat(address.postalCode).isEqualTo("94080-1912")
        verify(stripeNetworkClient, times(1)).executeRequest(any())
    }

    @Test
    fun `fetchPlace sends details request when autocomplete response only includes place id`() = runTest {
        val stripeNetworkClient = mock<StripeNetworkClient>()
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(
                code = 200,
                body = """
                    {
                      "suggestions": [
                        {
                          "display_data": {
                            "title": "354 Oyster Point Boulevard",
                            "subtitle": "South San Francisco, CA, USA"
                          },
                          "place_id": "places/ChIJr75gblx4j4AREm2k-W7EXcg"
                        }
                      ],
                      "source": "google"
                    }
                """.trimIndent(),
            ),
            StripeResponse(
                code = 200,
                body = """
                    {
                      "address": {
                        "line1": "354 Oyster Point Boulevard",
                        "line2": "",
                        "city": "South San Francisco",
                        "state": "CA",
                        "country": "US",
                        "postal_code": "94080-1912"
                      }
                    }
                """.trimIndent(),
            )
        )
        val proxy = createProxy(stripeNetworkClient = stripeNetworkClient)

        val prediction = proxy.findAutocompletePredictions(
            query = "354 Oys",
            country = "US",
            limit = 4,
        ).getOrThrow().autocompletePredictions.single()
        val address = proxy.fetchPlace(prediction.placeId).getOrThrow().place.transformGoogleToStripeAddress(Locale.US)

        val requestCaptor = argumentCaptor<StripeRequest>()
        verify(stripeNetworkClient, times(2)).executeRequest(requestCaptor.capture())
        val detailsRequest = requestCaptor.allValues[1] as ApiRequest
        val body = decodedBody(detailsRequest)
        assertThat(detailsRequest.url).isEqualTo("https://api.stripe.com/v1/elements/address/details")
        assertThat(body).contains("place_id=places/ChIJr75gblx4j4AREm2k-W7EXcg")
        assertThat(body).contains("session_token=session-token")
        assertThat(body).contains("client_type=mobile")
        assertThat(body).contains("source=google")
        assertThat(body).contains("display_title=354 Oyster Point Boulevard")
        assertThat(body).contains("locale=en-US")
        assertThat(body).contains("google_api_key=google-key")
        assertThat(address.line1).isEqualTo("354 Oyster Point Boulevard")
        assertThat(address.city).isEqualTo("South San Francisco")
    }

    @Test
    fun `fetchPlace returns failure when place id was not previously fetched`() = runTest {
        val proxy = createProxy(stripeNetworkClient = mock())

        val result = proxy.fetchPlace("missing-place-id")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Missing cached suggestion")
    }

    private fun createProxy(
        stripeNetworkClient: StripeNetworkClient,
        durationProvider: FakeDurationProvider = FakeDurationProvider(),
    ): StripeHostedPlacesClientProxy {
        return StripeHostedPlacesClientProxy(
            context = context,
            googleApiKey = "google-key",
            stripeNetworkClient = stripeNetworkClient,
            durationProvider = durationProvider,
            localeProvider = { Locale.US },
            optionsProvider = { ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            sessionTokenProvider = { "session-token" },
        )
    }

    private fun decodedBody(request: ApiRequest): String {
        return ByteArrayOutputStream().use { output ->
            request.writePostBody(output)
            URLDecoder.decode(output.toString(Charsets.UTF_8.name()), Charsets.UTF_8.name())
        }
    }
}

package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import javax.inject.Provider

internal class DefaultStripeAutocompleteRepository(
    private val stripeNetworkClient: StripeNetworkClient,
    private val apiRequestFactory: ApiRequest.Factory,
    private val requestOptionsProvider: Provider<ApiRequest.Options>,
) : StripeAutocompleteRepository {

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    override suspend fun findAutocompletePredictions(
        query: String,
        country: String,
        sessionToken: String,
        locale: String?
    ): Result<AutocompletePredictionsResult> {
        val params = buildMap<String, Any> {
            put("search_text", query)
            put("session_token", sessionToken)
            put("client_type", "mobile")
            put("country_codes", listOf(country))
            if (locale != null) {
                put("locale", locale)
            }
        }
        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = apiRequestFactory.createPost(
                url = AUTOCOMPLETE_URL,
                options = requestOptionsProvider.get(),
                params = params,
            ),
            responseJsonParser = AutocompletePredictionsResponseJsonParser,
        )
    }

    override suspend fun fetchPlaceDetails(
        placeId: String,
        sessionToken: String,
        locale: String?
    ): Result<PlaceDetailsResult> {
        val params = buildMap<String, Any> {
            put("place_id", placeId)
            put("session_token", sessionToken)
            put("client_type", "mobile")
            put("source", "google")
            if (locale != null) {
                put("locale", locale)
            }
        }
        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = apiRequestFactory.createPost(
                url = DETAILS_URL,
                options = requestOptionsProvider.get(),
                params = params,
            ),
            responseJsonParser = PlaceDetailsResponseJsonParser,
        )
    }

    companion object {
        private val AUTOCOMPLETE_URL: String
            get() = "${ApiRequest.API_HOST}/v1/elements/address/autocomplete"
        private val DETAILS_URL: String
            get() = "${ApiRequest.API_HOST}/v1/elements/address/details"
    }
}

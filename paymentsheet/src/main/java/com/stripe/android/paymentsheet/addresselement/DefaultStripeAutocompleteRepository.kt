package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser

internal class DefaultStripeAutocompleteRepository(
    private val stripeNetworkClient: StripeNetworkClient,
    private val apiRequestFactory: ApiRequest.Factory,
    private val publishableKeyProvider: () -> String,
    private val stripeAccountIdProvider: () -> String?
) : StripeAutocompleteRepository {

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    override suspend fun findAutocompletePredictions(
        query: String,
        country: String,
        sessionToken: String,
        locale: String?,
        googleApiKey: String?
    ): Result<AutocompletePredictionsResult> {
        val params = buildMap<String, Any> {
            put("search_text", query)
            put("session_token", sessionToken)
            put("client_type", "mobile")
            put("country_codes", listOf(country))
            if (locale != null) {
                put("locale", locale)
            }
            if (googleApiKey != null) {
                put("google_api_key", googleApiKey)
            }
        }
        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = apiRequestFactory.createPost(
                url = AUTOCOMPLETE_URL,
                options = ApiRequest.Options(
                    apiKey = publishableKeyProvider(),
                    stripeAccount = stripeAccountIdProvider(),
                ),
                params = params,
            ),
            responseJsonParser = AutocompletePredictionsResponseJsonParser,
        )
    }

    override suspend fun fetchPlaceDetails(
        placeId: String,
        sessionToken: String
    ): Result<PlaceDetailsResult> {
        val params = mapOf(
            "place_id" to placeId,
            "session_token" to sessionToken,
            "client_type" to "mobile",
            "source" to "google",
        )
        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = apiRequestFactory.createPost(
                url = DETAILS_URL,
                options = ApiRequest.Options(
                    apiKey = publishableKeyProvider(),
                    stripeAccount = stripeAccountIdProvider(),
                ),
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

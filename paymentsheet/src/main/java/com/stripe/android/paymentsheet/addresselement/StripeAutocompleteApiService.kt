package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.responseJson
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

internal interface StripeAutocompleteApiService {
    suspend fun findAutocompletePredictions(
        query: String,
        country: String,
        sessionToken: String,
        locale: String?,
        googleApiKey: String?
    ): Result<AutocompletePredictionsResult>

    suspend fun fetchPlaceDetails(
        placeId: String,
        sessionToken: String
    ): Result<PlaceDetailsResult>
}

internal data class AutocompletePredictionsResult(
    val predictions: List<AutocompleteSuggestion>
)

internal data class AutocompleteSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val address: StripeProxyAddress?
)

internal data class StripeProxyAddress(
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
)

internal data class PlaceDetailsResult(
    val address: StripeProxyAddress
)

internal class DefaultStripeAutocompleteApiService(
    private val stripeNetworkClient: StripeNetworkClient,
    private val apiRequestFactory: ApiRequest.Factory,
    private val publishableKeyProvider: () -> String,
    private val stripeAccountIdProvider: () -> String?
) : StripeAutocompleteApiService {

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
            put("country_codes[]", country)
            if (locale != null) {
                put("locale", locale)
            }
            if (googleApiKey != null) {
                put("google_api_key", googleApiKey)
            }
        }
        return executePost(AUTOCOMPLETE_URL, params) { json ->
            parseAutocompletePredictionsResponse(json)
        }
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
        return executePost(DETAILS_URL, params) { json ->
            parsePlaceDetailsResponse(json)
        }
    }

    private suspend fun <T> executePost(
        url: String,
        params: Map<String, Any>,
        parser: (JSONObject) -> T?
    ): Result<T> {
        val request = apiRequestFactory.createPost(
            url = url,
            options = ApiRequest.Options(
                apiKey = publishableKeyProvider(),
                stripeAccount = stripeAccountIdProvider(),
            ),
            params = params,
        )

        val response = try {
            stripeNetworkClient.executeRequest(request)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            return Result.failure(e)
        }

        return if (response.isError) {
            val stripeError = try {
                stripeErrorJsonParser.parse(response.responseJson())
            } catch (_: Exception) {
                null
            }
            Result.failure(
                Exception(
                    stripeError?.message
                        ?: "Request failed with status ${response.code}"
                )
            )
        } else {
            val parsed = try {
                parser(response.responseJson())
            } catch (_: Exception) {
                null
            }
            if (parsed != null) {
                Result.success(parsed)
            } else {
                Result.failure(Exception("Failed to parse response"))
            }
        }
    }

    companion object {
        private val AUTOCOMPLETE_URL: String
            get() = "${ApiRequest.API_HOST}/v1/elements/address/autocomplete"
        private val DETAILS_URL: String
            get() = "${ApiRequest.API_HOST}/v1/elements/address/details"
    }
}

internal fun parseAutocompletePredictionsResponse(
    json: JSONObject
): AutocompletePredictionsResult? {
    val suggestionsArray = json.optJSONArray("suggestions") ?: return null
    val predictions = (0 until suggestionsArray.length()).mapNotNull { i ->
        val suggestion = suggestionsArray.getJSONObject(i)
        val displayData = suggestion.optJSONObject("display_data")
        val primaryText = suggestion.optString("primary_text").ifEmpty {
            displayData?.optString("title").orEmpty()
        }
        val placeId = suggestion.optString("place_id")
        if (primaryText.isBlank() || placeId.isBlank()) return@mapNotNull null
        AutocompleteSuggestion(
            placeId = placeId,
            primaryText = primaryText,
            secondaryText = suggestion.optString("secondary_text").ifEmpty {
                displayData?.optString("subtitle").orEmpty()
            },
            address = suggestion.optJSONObject("address")?.let { parseAddress(it) },
        )
    }
    return AutocompletePredictionsResult(predictions = predictions)
}

private fun parsePlaceDetailsResponse(json: JSONObject): PlaceDetailsResult? {
    val addressJson = json.optJSONObject("address") ?: return null
    return PlaceDetailsResult(address = parseAddress(addressJson))
}

private fun parseAddress(json: JSONObject): StripeProxyAddress {
    return StripeProxyAddress(
        line1 = json.optString("line1").takeIf { it.isNotEmpty() },
        line2 = json.optString("line2").takeIf { it.isNotEmpty() },
        city = json.optString("city").takeIf { it.isNotEmpty() },
        state = json.optString("state").takeIf { it.isNotEmpty() },
        postalCode = json.optString("postal_code").takeIf { it.isNotEmpty() },
        country = json.optString("country").takeIf { it.isNotEmpty() },
    )
}

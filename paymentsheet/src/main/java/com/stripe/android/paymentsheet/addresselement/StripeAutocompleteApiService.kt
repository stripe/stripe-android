package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.responseJson
import kotlinx.parcelize.Parcelize
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

@Parcelize
internal data class AutocompletePredictionsResult(
    val predictions: List<AutocompleteSuggestion>
) : StripeModel

@Parcelize
internal data class AutocompleteSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val address: StripeProxyAddress?
) : StripeModel

@Parcelize
internal data class StripeProxyAddress(
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
) : StripeModel

@Parcelize
internal data class PlaceDetailsResult(
    val address: StripeProxyAddress
) : StripeModel

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
            AutocompletePredictionsResponseJsonParser.parse(json)
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
            PlaceDetailsResponseJsonParser.parse(json)
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

internal object AutocompletePredictionsResponseJsonParser :
    ModelJsonParser<AutocompletePredictionsResult> {
    override fun parse(json: JSONObject): AutocompletePredictionsResult {
        val suggestionsArray = json.optJSONArray("suggestions")
            ?: return AutocompletePredictionsResult(predictions = emptyList())
        val predictions = (0 until suggestionsArray.length()).mapNotNull { i ->
            val suggestion = suggestionsArray.optJSONObject(i)
                ?: return@mapNotNull null
            val displayData = suggestion.optJSONObject("display_data")
            val primaryText =
                StripeJsonUtils.optString(suggestion, "primary_text")
                    ?: StripeJsonUtils.optString(displayData, "title")
                    ?: return@mapNotNull null
            val placeId = StripeJsonUtils.optString(suggestion, "place_id")
                ?: return@mapNotNull null
            AutocompleteSuggestion(
                placeId = placeId,
                primaryText = primaryText,
                secondaryText =
                    StripeJsonUtils.optString(suggestion, "secondary_text")
                        ?: StripeJsonUtils.optString(displayData, "subtitle")
                        ?: "",
                address = suggestion.optJSONObject("address")
                    ?.let { parseAddress(it) },
            )
        }
        return AutocompletePredictionsResult(predictions = predictions)
    }
}

internal object PlaceDetailsResponseJsonParser : ModelJsonParser<PlaceDetailsResult> {
    override fun parse(json: JSONObject): PlaceDetailsResult? {
        val addressJson = json.optJSONObject("address") ?: return null
        return PlaceDetailsResult(address = parseAddress(addressJson))
    }
}

private fun parseAddress(json: JSONObject): StripeProxyAddress {
    return StripeProxyAddress(
        line1 = StripeJsonUtils.optString(json, "line1"),
        line2 = StripeJsonUtils.optString(json, "line2"),
        city = StripeJsonUtils.optString(json, "city"),
        state = StripeJsonUtils.optString(json, "state"),
        postalCode = StripeJsonUtils.optString(json, "postal_code"),
        country = StripeJsonUtils.optString(json, "country"),
    )
}

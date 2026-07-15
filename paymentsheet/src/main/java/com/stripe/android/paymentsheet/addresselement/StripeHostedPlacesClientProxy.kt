package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import android.text.SpannableString
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithKSerializerParser
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID

internal class StripeHostedPlacesClientProxy(
    context: Context,
    private val googleApiKey: String?,
    private val stripeNetworkClient: StripeNetworkClient = DefaultStripeNetworkClient(),
    private val durationProvider: DurationProvider = DefaultDurationProvider.instance,
    private val localeProvider: () -> Locale = { AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault() },
    private val apiRequestFactory: ApiRequest.Factory = ApiRequest.Factory(
        appInfo = Stripe.appInfo,
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
    ),
    private val optionsProvider: () -> ApiRequest.Options = {
        PaymentConfiguration.getInstance(context).let {
            ApiRequest.Options(
                apiKey = it.publishableKey,
                stripeAccount = it.stripeAccountId,
            )
        }
    },
    private val sessionTokenProvider: () -> String = { UUID.randomUUID().toString() },
) : PlacesClientProxy {
    private val sessionToken = sessionTokenProvider()
    private val stripeErrorJsonParser = StripeErrorJsonParser()
    private val json = Json { ignoreUnknownKeys = true }
    private val cachedSuggestions = linkedMapOf<String, CachedSuggestion>()

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int,
    ): Result<FindAutocompletePredictionsResponse> {
        return durationProvider.measureDuration(DurationProvider.Key.AutocompleteFindPredictions) {
            executePost(
                url = AUTOCOMPLETE_URL,
                params = buildAutocompleteParams(query = query, country = country),
                serializer = HostedAutocompleteResponse.serializer(),
            ).mapCatching { response ->
                cachedSuggestions.clear()
                val source = response.source?.ifBlank { null } ?: DEFAULT_SOURCE
                FindAutocompletePredictionsResponse(
                    autocompletePredictions = response.suggestions.take(limit).mapIndexed { index, suggestion ->
                        val placeId = suggestion.placeId ?: "hosted_${sessionToken}_$index"
                        cachedSuggestions[placeId] = CachedSuggestion(
                            source = source,
                            displayTitle = suggestion.displayData.title,
                            address = suggestion.address,
                        )
                        AutocompletePrediction(
                            primaryText = SpannableString(suggestion.displayData.title),
                            secondaryText = SpannableString(suggestion.displayData.subtitle),
                            placeId = placeId,
                        )
                    }
                )
            }
        }
    }

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        val suggestion = cachedSuggestions[placeId] ?: return Result.failure(
            IllegalStateException("Missing cached suggestion for placeId=$placeId")
        )

        suggestion.address?.let { address ->
            return Result.success(FetchPlaceResponse(place = address.toPlace()))
        }

        return executePost(
            url = DETAILS_URL,
            params = buildDetailsParams(
                placeId = placeId,
                source = suggestion.source,
                displayTitle = suggestion.displayTitle,
            ),
            serializer = HostedDetailsResponse.serializer(),
        ).mapCatching { response ->
            FetchPlaceResponse(place = response.address.toPlace())
        }
    }

    private suspend fun <Response> executePost(
        url: String,
        params: Map<String, *>,
        serializer: KSerializer<Response>,
    ): Result<Response> {
        return executeRequestWithKSerializerParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = apiRequestFactory.createPost(
                url = url,
                options = optionsProvider(),
                params = params,
            ),
            responseSerializer = serializer,
            json = json,
        )
    }

    private fun buildAutocompleteParams(
        query: String?,
        country: String,
    ): Map<String, *> = buildMap<String, Any> {
        put("search_text", query.orEmpty())
        put("session_token", sessionToken)
        put("client_type", CLIENT_TYPE)
        put("locale", localeProvider().toLanguageTag())
        if (country.isNotBlank()) {
            put("country_codes", listOf(country.lowercase(Locale.ROOT)))
        }
        googleApiKey?.takeIf { it.isNotBlank() }?.let {
            put("google_api_key", it)
        }
    }

    private fun buildDetailsParams(
        placeId: String,
        source: String,
        displayTitle: String,
    ): Map<String, *> = buildMap<String, Any> {
        put("place_id", placeId)
        put("session_token", sessionToken)
        put("client_type", CLIENT_TYPE)
        put("source", source)
        put("display_title", displayTitle)
        put("locale", localeProvider().toLanguageTag())
        googleApiKey?.takeIf { it.isNotBlank() }?.let {
            put("google_api_key", it)
        }
    }

    private data class CachedSuggestion(
        val source: String,
        val displayTitle: String,
        val address: HostedAddress?,
    )

    @Serializable
    private data class HostedAutocompleteResponse(
        val suggestions: List<HostedSuggestion> = emptyList(),
        val source: String? = null,
    )

    @Serializable
    private data class HostedSuggestion(
        @SerialName("display_data")
        val displayData: HostedDisplayData,
        @SerialName("place_id")
        val placeId: String? = null,
        val address: HostedAddress? = null,
    )

    @Serializable
    private data class HostedDisplayData(
        val title: String,
        val subtitle: String,
    )

    @Serializable
    private data class HostedDetailsResponse(
        val address: HostedAddress,
    )

    @Serializable
    private data class HostedAddress(
        val line1: String? = null,
        val line2: String? = null,
        val city: String? = null,
        val state: String? = null,
        val country: String? = null,
        @SerialName("postal_code")
        val postalCode: String? = null,
    ) {
        @Suppress("LongMethod")
        fun toPlace(): Place {
            return Place(
                addressComponents = buildList {
                    val (streetNumber, route) = splitLine1(line1)
                    streetNumber?.let {
                        add(
                            AddressComponent(
                                shortName = it,
                                longName = it,
                                types = listOf(Place.Type.STREET_NUMBER.value),
                            )
                        )
                    }
                    route?.let {
                        add(
                            AddressComponent(
                                shortName = it,
                                longName = it,
                                types = listOf(Place.Type.ROUTE.value),
                            )
                        )
                    }
                    line2?.takeIf { it.isNotBlank() }?.let {
                        add(
                            AddressComponent(
                                shortName = it,
                                longName = it,
                                types = listOf(Place.Type.PREMISE.value),
                            )
                        )
                    }
                    city?.takeIf { it.isNotBlank() }?.let {
                        add(
                            AddressComponent(
                                shortName = it,
                                longName = it,
                                types = listOf(Place.Type.LOCALITY.value),
                            )
                        )
                    }
                    state?.takeIf { it.isNotBlank() }?.let {
                        add(
                            AddressComponent(
                                shortName = it,
                                longName = it,
                                types = listOf(Place.Type.ADMINISTRATIVE_AREA_LEVEL_1.value),
                            )
                        )
                    }
                    country?.takeIf { it.isNotBlank() }?.let {
                        add(
                            AddressComponent(
                                shortName = it.uppercase(Locale.ROOT),
                                longName = it.uppercase(Locale.ROOT),
                                types = listOf(Place.Type.COUNTRY.value),
                            )
                        )
                    }
                    postalCode?.takeIf { it.isNotBlank() }?.let {
                        add(
                            AddressComponent(
                                shortName = it,
                                longName = it,
                                types = listOf(Place.Type.POSTAL_CODE.value),
                            )
                        )
                    }
                }.ifEmpty { null }
            )
        }
    }

    private companion object {
        private const val CLIENT_TYPE = "mobile"
        private const val DEFAULT_SOURCE = "google"
        private val AUTOCOMPLETE_URL = "${ApiRequest.API_HOST}/v1/elements/address/autocomplete"
        private val DETAILS_URL = "${ApiRequest.API_HOST}/v1/elements/address/details"

        private fun splitLine1(line1: String?): Pair<String?, String?> {
            val normalized = line1?.trim().orEmpty()
            if (normalized.isBlank()) {
                return null to null
            }
            val pieces = normalized.split("\\s+".toRegex(), limit = 2)
            val first = pieces.firstOrNull().orEmpty()
            return if (first.any(Char::isDigit)) {
                first to pieces.getOrNull(1)
            } else {
                null to normalized
            }
        }
    }
}

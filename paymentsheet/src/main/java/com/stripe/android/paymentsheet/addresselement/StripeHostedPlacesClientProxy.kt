package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import com.stripe.android.ui.core.elements.autocomplete.model.STRIPE_HOSTED_JAPANESE_LINE1
import com.stripe.android.ui.core.elements.autocomplete.model.STRIPE_HOSTED_JAPANESE_LINE2
import java.util.Locale
import java.util.UUID

internal class StripeHostedPlacesClientProxy(
    private val repository: StripeAutocompleteRepository,
) : PlacesClientProxy {
    private val lock = Any()
    private var sessionToken: String = newSessionToken()
    private val predictionCache = mutableMapOf<String, AutocompleteSuggestion>()

    override fun resetSession() {
        synchronized(lock) {
            sessionToken = newSessionToken()
            predictionCache.clear()
        }
    }

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int,
    ): Result<FindAutocompletePredictionsResponse> {
        val q = query ?: return Result.success(FindAutocompletePredictionsResponse(emptyList()))
        val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        val token = synchronized(lock) { sessionToken }
        return repository.findAutocompletePredictions(
            query = q,
            country = country,
            sessionToken = token,
            locale = locale.toLanguageTag(),
        ).map { result ->
            val limitedPredictions = result.predictions.take(limit)
            synchronized(lock) {
                predictionCache.clear()
                limitedPredictions.forEach { predictionCache[it.placeId] = it }
            }
            FindAutocompletePredictionsResponse(
                autocompletePredictions = limitedPredictions.map { suggestion ->
                    AutocompletePrediction(
                        primaryText = SpannableString(suggestion.primaryText),
                        secondaryText = SpannableString(suggestion.secondaryText),
                        placeId = suggestion.placeId,
                    )
                }
            )
        }
    }

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        val (cached, token) = synchronized(lock) {
            predictionCache[placeId] to sessionToken
        }
        if (cached?.address != null) {
            return Result.success(cached.address.toFetchPlaceResponse())
        }
        return repository.fetchPlaceDetails(
            placeId = placeId,
            sessionToken = token,
        ).map { result ->
            result.address.toFetchPlaceResponse()
        }
    }

    private fun StripeProxyAddress.toFetchPlaceResponse(): FetchPlaceResponse {
        val isJapaneseAddress = country == Locale.JAPAN.country
        val components = buildList {
            line1?.let { line1 ->
                add(
                    AddressComponent(
                        shortName = line1,
                        longName = line1,
                        types = listOf(if (isJapaneseAddress) STRIPE_HOSTED_JAPANESE_LINE1 else "route"),
                    )
                )
            }
            line2?.let { line2 ->
                add(
                    AddressComponent(
                        shortName = line2,
                        longName = line2,
                        types = listOf(if (isJapaneseAddress) STRIPE_HOSTED_JAPANESE_LINE2 else "premise"),
                    )
                )
            }
            city?.let { add(AddressComponent(shortName = it, longName = it, types = listOf("locality"))) }
            state?.let {
                add(AddressComponent(shortName = it, longName = it, types = listOf("administrative_area_level_1")))
            }
            postalCode?.let { add(AddressComponent(shortName = it, longName = it, types = listOf("postal_code"))) }
            country?.let { add(AddressComponent(shortName = it, longName = it, types = listOf("country"))) }
        }
        return FetchPlaceResponse(Place(addressComponents = components))
    }

    private companion object {
        fun newSessionToken(): String = UUID.randomUUID().toString()
    }
}

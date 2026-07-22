package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import java.util.Locale
import java.util.UUID

internal class StripeHostedPlacesClientProxy(
    private val repository: StripeAutocompleteRepository,
) : PlacesClientProxy {
    private val lock = Any()
    private var sessionToken: String = newSessionToken()
    private val predictionCache = mutableMapOf<String, AutocompleteSuggestion>()
    private var lastResolvedAddress: StripeProxyAddress? = null

    override fun resetSession() {
        synchronized(lock) {
            sessionToken = newSessionToken()
            predictionCache.clear()
            lastResolvedAddress = null
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
            synchronized(lock) { lastResolvedAddress = cached.address }
            return Result.success(FetchPlaceResponse(Place(addressComponents = null)))
        }
        return repository.fetchPlaceDetails(
            placeId = placeId,
            sessionToken = token,
        ).map { result ->
            synchronized(lock) { lastResolvedAddress = result.address }
            FetchPlaceResponse(Place(addressComponents = null))
        }
    }

    override fun transformToAddress(response: FetchPlaceResponse, locale: Locale): Address {
        val address = synchronized(lock) { lastResolvedAddress }
        return Address(
            line1 = address?.line1,
            line2 = address?.line2,
            city = address?.city,
            state = address?.state,
            postalCode = address?.postalCode,
            country = address?.country,
        )
    }

    private companion object {
        fun newSessionToken(): String = UUID.randomUUID().toString()
    }
}

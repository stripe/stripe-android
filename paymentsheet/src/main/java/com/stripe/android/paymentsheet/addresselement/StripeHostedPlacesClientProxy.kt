package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
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

    override suspend fun fetchPlace(placeId: String, locale: Locale): Result<Address> {
        val (cached, token) = synchronized(lock) {
            predictionCache[placeId] to sessionToken
        }
        if (cached?.address != null) {
            return Result.success(
                Address(
                    line1 = cached.address.line1,
                    line2 = cached.address.line2,
                    city = cached.address.city,
                    state = cached.address.state,
                    postalCode = cached.address.postalCode,
                    country = cached.address.country,
                )
            )
        }
        return repository.fetchPlaceDetails(
            placeId = placeId,
            sessionToken = token,
            locale = locale.toLanguageTag(),
        ).also { result ->
            result.onSuccess { details ->
                synchronized(lock) {
                    predictionCache[placeId]?.let { existing ->
                        predictionCache[placeId] = existing.copy(address = details.address)
                    }
                }
            }
        }.map { result ->
            Address(
                line1 = result.address?.line1,
                line2 = result.address?.line2,
                city = result.address?.city,
                state = result.address?.state,
                postalCode = result.address?.postalCode,
                country = result.address?.country,
            )
        }
    }

    private companion object {
        fun newSessionToken(): String = UUID.randomUUID().toString()
    }
}

package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import java.util.Locale
import java.util.UUID
import kotlin.time.TimeSource

internal class StripeHostedPlacesClientProxy(
    private val repository: StripeAutocompleteRepository,
    private val eventReporter: AddressLauncherEventReporter,
) : PlacesClientProxy {
    private val lock = Any()
    private var sessionToken: String = newSessionToken()
    private var lastQueryLength: Int = 0
    private val predictionCache = mutableMapOf<String, AutocompleteSuggestion>()

    init {
        eventReporter.onAutocompleteSessionStarted(sessionToken)
    }

    override fun resetSession() {
        val newToken = synchronized(lock) {
            newSessionToken().also {
                sessionToken = it
                lastQueryLength = 0
                predictionCache.clear()
            }
        }
        eventReporter.onAutocompleteSessionStarted(newToken)
    }

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int,
    ): Result<FindAutocompletePredictionsResponse> {
        val q = query ?: return Result.success(FindAutocompletePredictionsResponse(emptyList()))
        val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        val token = synchronized(lock) {
            lastQueryLength = q.length
            sessionToken
        }
        val fetchStart = TimeSource.Monotonic.markNow()
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
        }.onSuccess { response ->
            eventReporter.onAutocompleteSuggestionsReturned(
                sessionToken = token,
                resultCount = response.autocompletePredictions.size,
                fetchDuration = fetchStart.elapsedNow(),
            )
        }.onFailure { error ->
            eventReporter.onAutocompleteError(sessionToken = token, error = error)
        }
    }

    override suspend fun fetchPlace(placeId: String, locale: Locale): Result<Address> {
        val cached: AutocompleteSuggestion?
        val token: String
        val queryLength: Int
        synchronized(lock) {
            cached = predictionCache[placeId]
            token = sessionToken
            queryLength = lastQueryLength
        }
        eventReporter.onAutocompleteSelected(sessionToken = token, queryLength = queryLength, placeId = placeId)
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
        }.onFailure { error ->
            eventReporter.onAutocompleteError(sessionToken = token, error = error)
        }
    }

    private companion object {
        fun newSessionToken(): String = UUID.randomUUID().toString()
    }
}

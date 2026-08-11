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

internal class StripeHostedPlacesClientProxy(
    private val repository: StripeAutocompleteRepository,
    private val eventReporter: AddressLauncherEventReporter,
) : PlacesClientProxy {
    private val lock = Any()
    private var sessionToken: String = newSessionToken()
    private var lastQueryLength: Int = 0
    private var sessionStartReported: Boolean = false
    private var lastSource: String? = null
    private var lastCountry: String = ""
    private val predictionCache = mutableMapOf<String, AutocompleteSuggestion>()

    override fun resetSession() {
        synchronized(lock) {
            sessionToken = newSessionToken()
            lastQueryLength = 0
            sessionStartReported = false
            lastSource = null
            lastCountry = ""
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
        var isFirstQuery = false
        val token = synchronized(lock) {
            isFirstQuery = !sessionStartReported
            sessionStartReported = true
            lastQueryLength = q.length
            lastCountry = country
            sessionToken
        }
        if (isFirstQuery) {
            eventReporter.onAutocompleteSessionStarted(country = country, sessionToken = token)
        }
        eventReporter.onAutocompleteFetchStarted()
        var responseSource: String? = null
        return repository.findAutocompletePredictions(
            query = q,
            country = country,
            sessionToken = token,
            locale = locale.toLanguageTag(),
        ).map { result ->
            val limitedPredictions = result.predictions.take(limit)
            responseSource = result.source
            synchronized(lock) {
                lastSource = result.source
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
                country = country,
                sessionToken = token,
                resultCount = response.autocompletePredictions.size,
                source = responseSource,
            )
        }.onFailure { error ->
            eventReporter.onAutocompleteError(country = country, sessionToken = token, error = error)
        }
    }

    override suspend fun fetchPlace(placeId: String, locale: Locale): Result<Address> {
        val cached: AutocompleteSuggestion?
        val token: String
        val queryLength: Int
        val source: String?
        val country: String
        synchronized(lock) {
            cached = predictionCache[placeId]
            token = sessionToken
            queryLength = lastQueryLength
            source = lastSource
            country = lastCountry
        }
        if (cached?.address != null) {
            eventReporter.onAutocompleteSelected(
                country = country,
                sessionToken = token,
                queryLength = queryLength,
                placeId = placeId,
                source = source,
            )
            return Result.success(cached.address.toAddress())
        }
        eventReporter.onAutocompleteDetailsFetchStarted()
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
        }.map { it.address.toAddress() }.onSuccess {
            eventReporter.onAutocompleteSelected(
                country = country,
                sessionToken = token,
                queryLength = queryLength,
                placeId = placeId,
                source = source,
            )
        }.onFailure { error ->
            eventReporter.onAutocompleteError(country = country, sessionToken = token, error = error)
        }
    }

    private companion object {
        fun newSessionToken(): String = UUID.randomUUID().toString()
    }
}

private fun StripeProxyAddress?.toAddress(): Address = Address(
    line1 = this?.line1,
    line2 = this?.line2,
    city = this?.city,
    state = this?.state,
    postalCode = this?.postalCode,
    country = this?.country,
)

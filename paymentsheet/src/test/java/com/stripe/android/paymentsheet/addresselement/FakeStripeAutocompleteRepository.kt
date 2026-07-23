package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeStripeAutocompleteRepository : StripeAutocompleteRepository {
    var predictionsResult: Result<AutocompletePredictionsResult> = Result.success(
        AutocompletePredictionsResult(predictions = emptyList())
    )
    var detailsResult: Result<PlaceDetailsResult> = Result.failure(NotImplementedError())
    var onBeforeFindAutocompletePredictions: (suspend () -> Unit)? = null
    var onBeforeFetchPlaceDetails: (suspend () -> Unit)? = null

    data class FindPredictionsCall(
        val query: String,
        val country: String,
        val sessionToken: String,
        val locale: String?,
    )

    private val _findPredictionsCalls = Turbine<FindPredictionsCall>()
    val findPredictionsCalls: ReceiveTurbine<FindPredictionsCall> = _findPredictionsCalls

    data class FetchPlaceDetailsCall(
        val placeId: String,
        val sessionToken: String,
        val locale: String?,
    )

    private val _fetchPlaceDetailsCalls = Turbine<FetchPlaceDetailsCall>()
    val fetchPlaceDetailsCalls: ReceiveTurbine<FetchPlaceDetailsCall> = _fetchPlaceDetailsCalls

    override suspend fun findAutocompletePredictions(
        query: String,
        country: String,
        sessionToken: String,
        locale: String?
    ): Result<AutocompletePredictionsResult> {
        onBeforeFindAutocompletePredictions?.invoke()
        _findPredictionsCalls.add(FindPredictionsCall(query, country, sessionToken, locale))
        return predictionsResult
    }

    override suspend fun fetchPlaceDetails(
        placeId: String,
        sessionToken: String,
        locale: String?
    ): Result<PlaceDetailsResult> {
        _fetchPlaceDetailsCalls.add(FetchPlaceDetailsCall(placeId, sessionToken, locale))
        onBeforeFetchPlaceDetails?.invoke()
        return detailsResult
    }

    fun ensureAllEventsConsumed() {
        _findPredictionsCalls.ensureAllEventsConsumed()
        _fetchPlaceDetailsCalls.ensureAllEventsConsumed()
    }
}

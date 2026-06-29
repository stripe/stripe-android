package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place

internal class FakePlacesClientProxy(
    var findPredictionsResult: Result<FindAutocompletePredictionsResponse> =
        Result.success(FindAutocompletePredictionsResponse(emptyList())),
    var fetchPlaceResult: Result<FetchPlaceResponse> =
        Result.success(FetchPlaceResponse(Place(emptyList()))),
) : PlacesClientProxy {
    var onBeforeFindPredictions: (() -> Unit)? = null

    data class FindPredictionsCall(
        val query: String?,
        val country: String,
        val limit: Int,
    )

    private val _findPredictionsCalls = Turbine<FindPredictionsCall>()
    val findPredictionsCalls: ReceiveTurbine<FindPredictionsCall> = _findPredictionsCalls

    private val _fetchPlaceCalls = Turbine<String>()
    val fetchPlaceCalls: ReceiveTurbine<String> = _fetchPlaceCalls

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        onBeforeFindPredictions?.invoke()
        _findPredictionsCalls.add(FindPredictionsCall(query, country, limit))
        return findPredictionsResult
    }

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        _fetchPlaceCalls.add(placeId)
        return fetchPlaceResult
    }

    fun ensureAllEventsConsumed() {
        _findPredictionsCalls.ensureAllEventsConsumed()
        _fetchPlaceCalls.ensureAllEventsConsumed()
    }
}

package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import java.util.Locale

internal class FakePlacesClientProxy(
    var findPredictionsResult: Result<FindAutocompletePredictionsResponse>,
    var fetchPlaceResult: Result<Address>,
) : PlacesClientProxy {
    var onBeforeFindPredictions: (() -> Unit)? = null
    var onBeforeFetchPlace: (suspend () -> Unit)? = null

    data class FindPredictionsCall(
        val query: String?,
        val country: String,
        val limit: Int,
    )

    private val _findPredictionsCalls = Turbine<FindPredictionsCall>()
    val findPredictionsCalls: ReceiveTurbine<FindPredictionsCall> = _findPredictionsCalls

    data class FetchPlaceCall(val placeId: String, val locale: Locale)

    private val _fetchPlaceCalls = Turbine<FetchPlaceCall>()
    val fetchPlaceCalls: ReceiveTurbine<FetchPlaceCall> = _fetchPlaceCalls

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        onBeforeFindPredictions?.invoke()
        _findPredictionsCalls.add(FindPredictionsCall(query, country, limit))
        return findPredictionsResult
    }

    override suspend fun fetchPlace(placeId: String, locale: Locale): Result<Address> {
        _fetchPlaceCalls.add(FetchPlaceCall(placeId, locale))
        onBeforeFetchPlace?.invoke()
        return fetchPlaceResult
    }

    private val _resetSessionCalls = Turbine<Unit>()
    val resetSessionCalls: ReceiveTurbine<Unit> = _resetSessionCalls

    override fun resetSession() {
        _resetSessionCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        _findPredictionsCalls.ensureAllEventsConsumed()
        _fetchPlaceCalls.ensureAllEventsConsumed()
        _resetSessionCalls.ensureAllEventsConsumed()
    }
}

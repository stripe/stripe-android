package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import com.stripe.android.ui.core.elements.autocomplete.model.transformGoogleToStripeAddress
import java.util.Locale

internal class FakePlacesClientProxy(
    var findPredictionsResult: Result<FindAutocompletePredictionsResponse> =
        Result.success(FindAutocompletePredictionsResponse(emptyList())),
    var fetchPlaceResult: Result<FetchPlaceResponse> =
        Result.success(FetchPlaceResponse(Place(emptyList()))),
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

    private var lastFetchedResponse: FetchPlaceResponse? = null

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        _fetchPlaceCalls.add(placeId)
        onBeforeFetchPlace?.invoke()
        fetchPlaceResult.onSuccess { lastFetchedResponse = it }
        return fetchPlaceResult
    }

    private val _resetSessionCalls = Turbine<Unit>()
    val resetSessionCalls: ReceiveTurbine<Unit> = _resetSessionCalls

    override fun resetSession() {
        lastFetchedResponse = null
        _resetSessionCalls.add(Unit)
    }

    override fun transformToAddress(locale: Locale): Address {
        return lastFetchedResponse?.place?.transformGoogleToStripeAddress(locale) ?: Address()
    }

    fun ensureAllEventsConsumed() {
        _findPredictionsCalls.ensureAllEventsConsumed()
        _fetchPlaceCalls.ensureAllEventsConsumed()
        _resetSessionCalls.ensureAllEventsConsumed()
    }
}

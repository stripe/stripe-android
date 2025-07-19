package com.stripe.android.paymentsheet.utils

import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import kotlinx.coroutines.channels.Channel
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PlacesClientProxyTestRule : TestWatcher() {
    private val findAutocompletePredictionsResponseChannel =
        Channel<Result<FindAutocompletePredictionsResponse>>(capacity = 1)
    private val fetchPlaceResponseChannel = Channel<Result<FetchPlaceResponse>>(capacity = 1)

    override fun starting(description: Description?) {
        super.starting(description)
        PlacesClientProxy.override = FakePlacesClientProxy(
            findAutocompletePredictionsResponseChannel = findAutocompletePredictionsResponseChannel,
            fetchPlaceResponseChannel = fetchPlaceResponseChannel,
        )
    }

    override fun finished(description: Description?) {
        PlacesClientProxy.override = null
        super.finished(description)
    }

    fun enqueueFindAutocompletePredictionsResponse(
        response: Result<FindAutocompletePredictionsResponse>
    ) {
        findAutocompletePredictionsResponseChannel.trySend(response)
    }

    fun enqueueFetchPlaceResponse(
        response: Result<FetchPlaceResponse>
    ) {
        fetchPlaceResponseChannel.trySend(response)
    }

    private class FakePlacesClientProxy(
        private val findAutocompletePredictionsResponseChannel: Channel<Result<FindAutocompletePredictionsResponse>>,
        private val fetchPlaceResponseChannel: Channel<Result<FetchPlaceResponse>>,
    ) : PlacesClientProxy {
        override suspend fun findAutocompletePredictions(
            query: String?,
            country: String,
            limit: Int
        ): Result<FindAutocompletePredictionsResponse> {
            return findAutocompletePredictionsResponseChannel.receive()
        }

        override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
            return fetchPlaceResponseChannel.receive()
        }
    }
}

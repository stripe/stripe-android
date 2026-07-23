package com.stripe.android.paymentsheet.utils

import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PlacesClientProxyTestRule : TestWatcher() {
    private val findAutocompletePredictionsResponseChannel =
        Channel<Result<FindAutocompletePredictionsResponse>>(capacity = 1)
    private val fetchPlaceResponseChannel = Channel<Result<Address>>(capacity = 1)

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
        response: Result<Address>
    ) {
        fetchPlaceResponseChannel.trySend(response)
    }

    private class FakePlacesClientProxy(
        private val findAutocompletePredictionsResponseChannel: Channel<Result<FindAutocompletePredictionsResponse>>,
        private val fetchPlaceResponseChannel: Channel<Result<Address>>,
    ) : PlacesClientProxy {
        override fun resetSession() = Unit

        override suspend fun findAutocompletePredictions(
            query: String?,
            country: String,
            limit: Int
        ): Result<FindAutocompletePredictionsResponse> {
            return findAutocompletePredictionsResponseChannel.receive()
        }

        override suspend fun fetchPlace(placeId: String, locale: Locale): Result<Address> {
            return fetchPlaceResponseChannel.receive()
        }
    }
}

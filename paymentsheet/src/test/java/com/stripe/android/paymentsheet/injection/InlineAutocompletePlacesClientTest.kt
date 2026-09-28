package com.stripe.android.paymentsheet.injection

import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.addresselement.FakePlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InlineAutocompletePlacesClientTest {

    @Test
    fun `resetSession delegates to resolved client`() = runTest {
        val fakePlacesClient = FakePlacesClientProxy(
            findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
            fetchPlaceResult = Result.success(Address()),
        )
        val client = LazyPlacesClientProxy { fakePlacesClient }

        client.resetSession()

        fakePlacesClient.resetSessionCalls.awaitItem()
        fakePlacesClient.ensureAllEventsConsumed()
    }
}

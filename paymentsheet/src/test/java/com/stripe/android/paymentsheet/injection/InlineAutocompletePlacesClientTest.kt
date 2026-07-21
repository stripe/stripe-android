package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.addresselement.FakePlacesClientProxy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InlineAutocompletePlacesClientTest {

    @Test
    fun `resetSession delegates to resolved client`() = runTest {
        val fakePlacesClient = FakePlacesClientProxy()
        val client = LazyPlacesClientProxy { fakePlacesClient }

        client.resetSession()

        fakePlacesClient.resetSessionCalls.awaitItem()
        fakePlacesClient.ensureAllEventsConsumed()
    }
}

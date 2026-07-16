package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StripeHostedPlacesClientProxyTest {
    @Test
    fun `findAutocompletePredictions returns failure`() = runTest {
        val proxy = StripeHostedPlacesClientProxy()

        val result = proxy.findAutocompletePredictions(
            query = "123 Main",
            country = "US",
            limit = 4,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
    }

    @Test
    fun `fetchPlace returns failure`() = runTest {
        val proxy = StripeHostedPlacesClientProxy()

        val result = proxy.fetchPlace(placeId = "ChIJ123")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
    }
}

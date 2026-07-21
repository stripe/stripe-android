package com.stripe.android.paymentsheet.injection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.FakePlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import org.junit.Test

class InlineAutocompletePlacesClientTest {

    @Test
    fun `resetSession delegates to resolved client`() {
        val fakePlacesClient = FakePlacesClientProxy()
        val client = createLazyPlacesClientProxy {
            fakePlacesClient
        }

        client.resetSession()

        assertThat(fakePlacesClient.resetSessionCallCount).isEqualTo(1)
    }

    private fun createLazyPlacesClientProxy(factory: () -> PlacesClientProxy): PlacesClientProxy {
        val constructor = Class.forName("com.stripe.android.paymentsheet.injection.LazyPlacesClientProxy")
            .getDeclaredConstructor(kotlin.jvm.functions.Function0::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(factory) as PlacesClientProxy
    }
}

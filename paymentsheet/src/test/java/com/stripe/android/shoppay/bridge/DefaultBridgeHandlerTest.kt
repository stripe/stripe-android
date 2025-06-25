package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class DefaultBridgeHandlerTest {

    @Test
    fun `getStripePublishableKey returns the publishable key provided in constructor`() {
        val handler = createDefaultBridgeHandler()

        val actualKey = handler.getStripePublishableKey()

        assertThat(actualKey).isEqualTo(PUBLISHABLE_KEY)
    }

    private fun createDefaultBridgeHandler(): DefaultBridgeHandler {
        return DefaultBridgeHandler(PUBLISHABLE_KEY)
    }

    companion object {
        const val PUBLISHABLE_KEY = "pk_test_123"
    }
}

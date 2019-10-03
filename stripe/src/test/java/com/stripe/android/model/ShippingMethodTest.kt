package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [ShippingMethod]
 */
class ShippingMethodTest {

    @Test
    fun testEquals() {
        assertEquals(SHIPPING_METHOD, createShippingMethod())
    }

    @Test
    fun testHashcode() {
        assertEquals(SHIPPING_METHOD.hashCode(), createShippingMethod().hashCode())
    }

    companion object {
        private val SHIPPING_METHOD = createShippingMethod()

        private fun createShippingMethod(): ShippingMethod {
            return ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD")
        }
    }
}

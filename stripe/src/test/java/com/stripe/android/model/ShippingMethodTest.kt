package com.stripe.android.model

import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [ShippingMethod]
 */
@RunWith(RobolectricTestRunner::class)
class ShippingMethodTest {

    @Test
    fun testEquals() {
        assertEquals(SHIPPING_METHOD, createShippingMethod())
    }

    @Test
    fun testParcel() {
        assertEquals(SHIPPING_METHOD, ParcelUtils.create(SHIPPING_METHOD))
    }

    companion object {
        private val SHIPPING_METHOD = createShippingMethod()

        private fun createShippingMethod(): ShippingMethod {
            return ShippingMethod("FedEx", "fedex", 599, "USD", "Arrives tomorrow")
        }
    }
}

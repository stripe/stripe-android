package com.stripe.android.view

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ShippingMethodAdapterTest {
    private val adapter: ShippingMethodAdapter = ShippingMethodAdapter()

    @Test
    fun selectedShippingMethod_withNoItems_returnsNull() {
        assertNull(adapter.selectedShippingMethod)
    }

    @Test
    fun selectedIndex_whenValueChanges_shouldInvokeCallback() {
        adapter.shippingMethods = listOf(
            ShippingMethodFixtures.UPS,
            ShippingMethodFixtures.FEDEX
        )
        var callbackCount = 0
        adapter.onShippingMethodSelectedCallback = {
            callbackCount += 1
        }

        adapter.selectedIndex = 0
        assertEquals(0, callbackCount)

        adapter.selectedIndex = 1
        assertEquals(1, callbackCount)

        adapter.selectedIndex = 1
        adapter.selectedIndex = 1
        adapter.selectedIndex = 1
        assertEquals(1, callbackCount)

        adapter.selectedIndex = 0
        assertEquals(2, callbackCount)
    }
}

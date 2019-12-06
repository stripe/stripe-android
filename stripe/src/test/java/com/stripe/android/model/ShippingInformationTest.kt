package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ShippingInformationTest {

    @Test
    fun testEquals() {
        assertEquals(createShippingInformation(), createShippingInformation())
    }

    @Test
    fun testToParamMapStripsNulls() {
        assertEquals(
            mapOf("name" to "home"),
            ShippingInformation(null, "home", null).toParamMap()
        )
    }

    private fun createShippingInformation(): ShippingInformation {
        return ShippingInformation(AddressFixtures.ADDRESS, "home", "555-123-4567")
    }
}

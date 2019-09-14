package com.stripe.android.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingInformationTest {

    @Test
    fun testEquals() {
        assertEquals(createShippingInformation(), createShippingInformation())
    }

    private fun createShippingInformation(): ShippingInformation {
        return ShippingInformation(Address.fromJson(AddressFixtures.ADDRESS_JSON),
            "home", "555-123-4567")
    }
}

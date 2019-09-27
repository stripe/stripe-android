package com.stripe.android.model

import com.stripe.android.testharness.JsonTestUtils.assertMapEquals
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [Address].
 */
class AddressTest {

    @Test
    fun fromJsonString_toParamMap_createsExpectedParamMap() {
        assertMapEquals(MAP_ADDRESS, ADDRESS.toParamMap())
    }

    @Test
    fun builderConstructor_whenCalled_createsExpectedAddress() {
        val address = Address.Builder()
            .setCity("San Francisco")
            .setCountry("US")
            .setLine1("123 Market St")
            .setLine2("#345")
            .setPostalCode("94107")
            .setState("CA")
            .build()
        assertEquals(address, ADDRESS)
    }

    @Test
    fun toParamMap_shouldRejectEmptyValues() {
        val addressParams = Address.Builder()
            .setCountry("US")
            .build()
            .toParamMap()
        assertEquals(mapOf("country" to "US"), addressParams)
    }

    companion object {
        private val MAP_ADDRESS = mapOf(
            "city" to "San Francisco",
            "country" to "US",
            "line1" to "123 Market St",
            "line2" to "#345",
            "postal_code" to "94107",
            "state" to "CA"
        )

        private val ADDRESS = Address.fromJson(AddressFixtures.ADDRESS_JSON)!!
    }
}

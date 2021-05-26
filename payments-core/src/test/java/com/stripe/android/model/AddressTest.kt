package com.stripe.android.model

import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [Address].
 */
@RunWith(RobolectricTestRunner::class)
class AddressTest {

    @Test
    fun fromJsonString_toParamMap_createsExpectedParamMap() {
        assertEquals(MAP_ADDRESS, AddressFixtures.ADDRESS.toParamMap())
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
        assertEquals(address, AddressFixtures.ADDRESS)
    }

    @Test
    fun toParamMap_shouldRejectEmptyValues() {
        val addressParams = Address.Builder()
            .setCountry("US")
            .build()
            .toParamMap()
        assertEquals(mapOf("country" to "US"), addressParams)
    }

    @Test
    fun testParcelize() {
        assertEquals(AddressFixtures.ADDRESS, ParcelUtils.create(AddressFixtures.ADDRESS))
    }

    private companion object {
        private val MAP_ADDRESS = mapOf(
            "city" to "San Francisco",
            "country" to "US",
            "line1" to "123 Market St",
            "line2" to "#345",
            "postal_code" to "94107",
            "state" to "CA"
        )
    }
}

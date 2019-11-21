package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GooglePayResultTest {

    @Test
    fun fromJson_withFullBillingAddress() {
        val result = GooglePayResult.fromJson(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS
        )
        val expectedAddress = Address(
            line1 = "510 Townsend St",
            city = "San Francisco",
            state = "CA",
            country = "US",
            postalCode = "94103"
        )

        assertEquals("tok_1F4VSjBbvEcIpqUbSsbEtBap", result.token?.id)
        assertEquals("card", result.token?.type)

        assertEquals(expectedAddress, result.address)

        assertEquals("Stripe Johnson", result.name)
        assertEquals("stripe@example.com", result.email)
        assertEquals("1-888-555-1234", result.phoneNumber)
    }

    @Test
    fun fromJson_withNoBillingAddress() {
        val result = GooglePayResult.fromJson(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS
        )

        assertEquals("tok_1F4ACMCRMbs6FrXf6fPqLnN7", result.token?.id)
        assertEquals("card", result.token?.type)
        assertNull(result.address)
        assertNull(result.name)
        assertNull(result.email)
        assertNull(result.phoneNumber)
    }
}

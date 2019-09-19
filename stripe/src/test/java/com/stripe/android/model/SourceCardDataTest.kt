package com.stripe.android.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test class for [SourceCardData].
 */
class SourceCardDataTest {

    @Test
    fun fromExampleJsonCard_createsExpectedObject() {
        assertEquals(Card.CardBrand.VISA, CARD_DATA.brand)
        assertEquals(Card.FundingType.CREDIT, CARD_DATA.funding)
        assertEquals("4242", CARD_DATA.last4)
        assertNotNull(CARD_DATA.expiryMonth)
        assertNotNull(CARD_DATA.expiryYear)
        assertEquals(12, CARD_DATA.expiryMonth!!.toInt().toLong())
        assertEquals(2050, CARD_DATA.expiryYear!!.toInt().toLong())
        assertEquals("US", CARD_DATA.country)
        assertEquals("optional", CARD_DATA.threeDSecureStatus)
        assertEquals("apple_pay", CARD_DATA.tokenizationMethod)
    }

    @Test
    fun testEquals() {
        assertEquals(CARD_DATA,
            SourceCardData.fromJson(SOURCE_CARD_DATA_WITH_APPLE_PAY))
    }

    @Test
    fun testHashCode() {
        assertNotNull(CARD_DATA)
        assertEquals(
            CARD_DATA.hashCode().toLong(),
            SourceCardData.fromJson(SOURCE_CARD_DATA_WITH_APPLE_PAY)!!.hashCode().toLong()
        )
    }

    @Test
    fun testAsThreeDSecureStatus() {
        assertEquals(SourceCardData.ThreeDSecureStatus.REQUIRED,
            SourceCardData.asThreeDSecureStatus("required"))
        assertEquals(SourceCardData.ThreeDSecureStatus.OPTIONAL,
            SourceCardData.asThreeDSecureStatus("optional"))
        assertEquals(SourceCardData.ThreeDSecureStatus.NOT_SUPPORTED,
            SourceCardData.asThreeDSecureStatus("not_supported"))
        assertEquals(SourceCardData.ThreeDSecureStatus.RECOMMENDED,
            SourceCardData.asThreeDSecureStatus("recommended"))
        assertEquals(SourceCardData.ThreeDSecureStatus.UNKNOWN,
            SourceCardData.asThreeDSecureStatus("unknown"))
        assertNull(SourceCardData.asThreeDSecureStatus(""))
    }

    companion object {

        @JvmField
        internal val SOURCE_CARD_DATA_WITH_APPLE_PAY = JSONObject(
            """
            {
                "exp_month": 12,
                "exp_year": 2050,
                "address_line1_check": "unchecked",
                "address_zip_check": "unchecked",
                "brand": "Visa",
                "country": "US",
                "cvc_check": "unchecked",
                "funding": "credit",
                "last4": "4242",
                "three_d_secure": "optional",
                "tokenization_method": "apple_pay",
                "dynamic_last4": "4242"
            }
            """.trimIndent()
        )

        private val CARD_DATA = SourceCardData.fromJson(SOURCE_CARD_DATA_WITH_APPLE_PAY)!!
    }
}

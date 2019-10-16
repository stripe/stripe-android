package com.stripe.android.model

import com.stripe.android.model.SourceFixtures.SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        assertEquals(12, CARD_DATA.expiryMonth)
        assertEquals(2050, CARD_DATA.expiryYear)
        assertEquals("US", CARD_DATA.country)
        assertEquals("optional", CARD_DATA.threeDSecureStatus)
        assertEquals("apple_pay", CARD_DATA.tokenizationMethod)
    }

    @Test
    fun testEquals() {
        assertEquals(CARD_DATA,
            SourceCardData.fromJson(SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON))
    }

    @Test
    fun testHashCode() {
        assertNotNull(CARD_DATA)
        assertEquals(
            CARD_DATA.hashCode(),
            SourceCardData.fromJson(SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON).hashCode()
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
        private val CARD_DATA =
            SourceCardData.fromJson(SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON)!!
    }
}

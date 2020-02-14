package com.stripe.android.model.parsers

import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.SourceTypeModel
import com.stripe.android.model.TokenizationMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SourceCardDataJsonParserTest {
    @Test
    fun testAsThreeDSecureStatus() {
        assertEquals(SourceTypeModel.Card.ThreeDSecureStatus.REQUIRED,
            SourceCardDataJsonParser.asThreeDSecureStatus("required"))
        assertEquals(SourceTypeModel.Card.ThreeDSecureStatus.OPTIONAL,
            SourceCardDataJsonParser.asThreeDSecureStatus("optional"))
        assertEquals(SourceTypeModel.Card.ThreeDSecureStatus.NOT_SUPPORTED,
            SourceCardDataJsonParser.asThreeDSecureStatus("not_supported"))
        assertEquals(SourceTypeModel.Card.ThreeDSecureStatus.RECOMMENDED,
            SourceCardDataJsonParser.asThreeDSecureStatus("recommended"))
        assertEquals(SourceTypeModel.Card.ThreeDSecureStatus.UNKNOWN,
            SourceCardDataJsonParser.asThreeDSecureStatus("unknown"))
        assertNull(SourceCardDataJsonParser.asThreeDSecureStatus(""))
    }

    @Test
    fun fromExampleJsonCard_createsExpectedObject() {
        assertEquals(CardBrand.Visa, CARD_DATA.brand)
        assertEquals(CardFunding.Credit, CARD_DATA.funding)
        assertEquals("4242", CARD_DATA.last4)
        assertNotNull(CARD_DATA.expiryMonth)
        assertNotNull(CARD_DATA.expiryYear)
        assertEquals(12, CARD_DATA.expiryMonth)
        assertEquals(2050, CARD_DATA.expiryYear)
        assertEquals("US", CARD_DATA.country)
        assertEquals("optional", CARD_DATA.threeDSecureStatus)
        assertEquals(TokenizationMethod.ApplePay, CARD_DATA.tokenizationMethod)
    }

    @Test
    fun testEquals() {
        assertEquals(CARD_DATA,
            PARSER.parse(SourceFixtures.SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON))
    }

    @Test
    fun testHashCode() {
        assertNotNull(CARD_DATA)
        assertEquals(
            CARD_DATA.hashCode(),
            PARSER.parse(SourceFixtures.SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON).hashCode()
        )
    }

    private companion object {
        private val PARSER = SourceCardDataJsonParser()
        private val CARD_DATA =
            PARSER.parse(SourceFixtures.SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON)
    }
}

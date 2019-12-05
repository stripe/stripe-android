package com.stripe.android.model

import com.stripe.android.model.parsers.CardJsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.json.JSONObject

/**
 * Test class for [StripeModel].
 */
class StripeModelTest {

    @Test
    fun equals_whenEquals_returnsTrue() {
        assertTrue(StripeModel::class.java.isAssignableFrom(Card::class.java))

        val firstCard = parse(CardTest.JSON_CARD_USD)
        val secondCard = parse(CardTest.JSON_CARD_USD)

        assertEquals(firstCard, secondCard)
        // Just confirming for sanity
        assertNotSame(firstCard, secondCard)
    }

    @Test
    fun equals_whenNotEquals_returnsFalse() {
        assertTrue(StripeModel::class.java.isAssignableFrom(Card::class.java))
        val firstCard = Card.create("4242", null, null, null)
        val secondCard = Card.create("4343", null, null, null)
        assertNotEquals(firstCard, secondCard)
    }

    @Test
    fun hashCode_whenEquals_returnsSameValue() {
        assertTrue(StripeModel::class.java.isAssignableFrom(Card::class.java))

        val firstCard = parse(CardTest.JSON_CARD_USD)
        val secondCard = parse(CardTest.JSON_CARD_USD)
        assertNotNull(firstCard)
        assertNotNull(secondCard)

        assertEquals(firstCard.hashCode(), secondCard.hashCode())
    }

    @Test
    fun hashCode_whenNotEquals_returnsDifferentValues() {
        assertTrue(StripeModel::class.java.isAssignableFrom(Card::class.java))

        val usdCard = CardFixtures.CARD_USD
        val eurCard = CardFixtures.CARD_EUR

        assertNotNull(usdCard)
        assertNotNull(eurCard)

        assertNotEquals(usdCard.hashCode(), eurCard.hashCode())
    }

    private fun parse(json: JSONObject): Card? {
        return requireNotNull(CardJsonParser().parse(json))
    }
}

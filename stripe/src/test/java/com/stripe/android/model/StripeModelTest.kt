package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.parsers.CardJsonParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.json.JSONObject

/**
 * Test class for [StripeModel].
 */
class StripeModelTest {

    @BeforeTest
    fun setup() {
        assertTrue(StripeModel::class.java.isAssignableFrom(Card::class.java))
    }

    @Test
    fun equals_whenEquals_returnsTrue() {
        val firstCard = parse(CardTest.JSON_CARD_USD)
        val secondCard = parse(CardTest.JSON_CARD_USD)

        assertEquals(firstCard, secondCard)
        // Just confirming for sanity
        assertNotSame(firstCard, secondCard)
    }

    @Test
    fun equals_whenNotEquals_returnsFalse() {
        assertThat(CardParams("4242", 1, 2021))
            .isNotEqualTo(CardParams("4343", 1, 20201))
    }

    @Test
    fun hashCode_whenEquals_returnsSameValue() {
        assertThat(parse(CardTest.JSON_CARD_USD).hashCode())
            .isEqualTo(parse(CardTest.JSON_CARD_USD).hashCode())
    }

    @Test
    fun hashCode_whenNotEquals_returnsDifferentValues() {
        assertThat(CardFixtures.CARD_USD.hashCode())
            .isNotEqualTo(CardFixtures.CARD_EUR.hashCode())
    }

    private fun parse(json: JSONObject): Card {
        return requireNotNull(CardJsonParser().parse(json))
    }
}

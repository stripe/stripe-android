package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.parsers.CardJsonParser
import org.json.JSONObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

/**
 * Test class for [StripeModel].
 */
class StripeModelTest {

    @BeforeTest
    fun setup() {
        assertThat(StripeModel::class.java.isAssignableFrom(Card::class.java))
            .isTrue()
    }

    @Test
    fun equals_whenEquals_returnsTrue() {
        val firstCard = parse(CardFixtures.CARD_USD_JSON)
        val secondCard = parse(CardFixtures.CARD_USD_JSON)

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
        assertThat(parse(CardFixtures.CARD_USD_JSON).hashCode())
            .isEqualTo(parse(CardFixtures.CARD_USD_JSON).hashCode())
    }

    @Test
    fun hashCode_whenNotEquals_returnsDifferentValues() {
        assertThat(CardFixtures.CARD_USD.hashCode())
            .isNotEqualTo(
                CardFixtures.CARD_USD
                    .copy(currency = "eur")
                    .hashCode()
            )
    }

    private fun parse(json: JSONObject): Card {
        return requireNotNull(CardJsonParser().parse(json))
    }
}

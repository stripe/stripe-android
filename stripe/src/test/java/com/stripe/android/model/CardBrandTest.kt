package com.stripe.android.model

import com.stripe.android.CardNumberFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardBrandTest {

    @Test
    fun fromCardNumber_withNull() {
        assertEquals(
            CardBrand.Unknown,
            CardBrand.fromCardNumber(null)
        )
    }

    @Test
    fun fromCardNumber_withEmpty() {
        assertEquals(
            CardBrand.Unknown,
            CardBrand.fromCardNumber("")
        )
    }

    @Test
    fun fromCardNumber_withAmericanExpress() {
        assertEquals(
            CardBrand.AmericanExpress,
            CardBrand.fromCardNumber(CardNumberFixtures.VALID_AMEX_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withDinersClub() {
        assertEquals(
            CardBrand.DinersClub,
            CardBrand.fromCardNumber(CardNumberFixtures.VALID_DINERS_CLUB_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withVisa() {
        assertEquals(
            CardBrand.Visa,
            CardBrand.fromCardNumber(CardNumberFixtures.VALID_VISA_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withInvalidVisa() {
        assertEquals(
            CardBrand.Unknown,
            CardBrand.fromCardNumber("1" + CardNumberFixtures.VALID_VISA_NO_SPACES)
        )
    }

    @Test
    fun isValidCardLengthWithBrand_whenBrandUnknown_alwaysReturnsFalse() {
        // Adding this check to ensure the input number is correct
        assertTrue(
            CardBrand.Visa.isValidCardNumberLength(CardNumberFixtures.VALID_VISA_NO_SPACES)
        )
        assertFalse(
            CardBrand.Unknown.isValidCardNumberLength(CardNumberFixtures.VALID_VISA_NO_SPACES)
        )
    }
}

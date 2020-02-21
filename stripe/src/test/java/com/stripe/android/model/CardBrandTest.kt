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
            CardBrand.fromCardNumber(CardNumberFixtures.AMEX_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withDinersClub14() {
        assertEquals(
            CardBrand.DinersClub,
            CardBrand.fromCardNumber(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withDinersClub16() {
        assertEquals(
            CardBrand.DinersClub,
            CardBrand.fromCardNumber(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withJcb() {
        assertEquals(
            CardBrand.JCB,
            CardBrand.fromCardNumber(CardNumberFixtures.JCB_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withVisa() {
        assertEquals(
            CardBrand.Visa,
            CardBrand.fromCardNumber(CardNumberFixtures.VISA_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withInvalidVisa() {
        assertEquals(
            CardBrand.Unknown,
            CardBrand.fromCardNumber("1" + CardNumberFixtures.VISA_NO_SPACES)
        )
    }

    @Test
    fun isValidCardLengthWithBrand_whenBrandUnknown_alwaysReturnsFalse() {
        // Adding this check to ensure the input number is correct
        assertTrue(
            CardBrand.Visa.isValidCardNumberLength(CardNumberFixtures.VISA_NO_SPACES)
        )
        assertTrue(
            CardBrand.DinersClub.isValidCardNumberLength(
                CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
            )
        )
        assertTrue(
            CardBrand.DinersClub.isValidCardNumberLength(
                CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
            )
        )
        assertFalse(
            CardBrand.Unknown.isValidCardNumberLength(CardNumberFixtures.VISA_NO_SPACES)
        )
    }

    @Test
    fun isMaxCvc_whenThreeDigitsAndNotAmEx_returnsTrue() {
        assertTrue(CardBrand.Visa.isMaxCvc("123"))
        assertTrue(CardBrand.MasterCard.isMaxCvc("345"))
        assertTrue(CardBrand.JCB.isMaxCvc("678"))
        assertTrue(CardBrand.DinersClub.isMaxCvc("910"))
        assertTrue(CardBrand.Discover.isMaxCvc("234"))
        assertTrue(CardBrand.Unknown.isMaxCvc("3333"))
    }

    @Test
    fun isMaxCvc_whenThreeDigitsAndIsAmEx_returnsFalse() {
        assertFalse(CardBrand.AmericanExpress.isMaxCvc("123"))
    }

    @Test
    fun isMaxCvc_whenFourDigitsAndIsAmEx_returnsTrue() {
        assertTrue(CardBrand.AmericanExpress.isMaxCvc("1234"))
    }

    @Test
    fun isMaxCvc_whenTooManyDigits_returnsFalse() {
        assertFalse(CardBrand.AmericanExpress.isMaxCvc("12345"))
        assertFalse(CardBrand.Visa.isMaxCvc("1234"))
        assertFalse(CardBrand.MasterCard.isMaxCvc("123456"))
        assertFalse(CardBrand.DinersClub.isMaxCvc("1234567"))
        assertFalse(CardBrand.Discover.isMaxCvc("12345678"))
        assertFalse(CardBrand.JCB.isMaxCvc("123456789012345"))
    }

    @Test
    fun isMaxCvc_whenNotEnoughDigits_returnsFalse() {
        assertFalse(CardBrand.AmericanExpress.isMaxCvc(""))
        assertFalse(CardBrand.Visa.isMaxCvc("1"))
        assertFalse(CardBrand.MasterCard.isMaxCvc("12"))
        assertFalse(CardBrand.DinersClub.isMaxCvc(""))
        assertFalse(CardBrand.Discover.isMaxCvc("8"))
        assertFalse(CardBrand.JCB.isMaxCvc("1"))
    }

    @Test
    fun isMaxCvc_whenWhitespaceAndNotEnoughDigits_returnsFalse() {
        assertFalse(CardBrand.AmericanExpress.isMaxCvc("   "))
        assertFalse(CardBrand.Visa.isMaxCvc("  1"))
    }

    @Test
    fun isMaxCvc_whenNull_returnsFalse() {
        assertFalse(CardBrand.AmericanExpress.isMaxCvc(null))
    }
}

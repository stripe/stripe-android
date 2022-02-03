package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardBrandTest {

    @Test
    fun fromCardNumber_withNull() {
        assertEquals(
            com.stripe.android.ui.core.elements.CardBrand.Unknown,
            com.stripe.android.ui.core.elements.CardBrand.fromCardNumber(null)
        )
    }

    @Test
    fun fromCardNumber_withEmpty() {
        assertEquals(
            com.stripe.android.ui.core.elements.CardBrand.Unknown,
            com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("")
        )
    }

    @Test
    fun fromCardNumber_withAmericanExpress() {
        assertEquals(
            com.stripe.android.ui.core.elements.CardBrand.AmericanExpress,
            com.stripe.android.ui.core.elements.CardBrand.fromCardNumber(CardNumberFixtures.AMEX_NO_SPACES)
        )
    }

    @Test
    fun fromCardNumber_withDinersClub14() {
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.DinersClub)
    }

    @Test
    fun fromCardNumber_withDinersClub16() {
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.DinersClub)
    }

    @Test
    fun fromCardNumber_withJcb() {
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber(CardNumberFixtures.JCB_NO_SPACES))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.JCB)
    }

    @Test
    fun fromCardNumber_withVisa() {
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber(CardNumberFixtures.VISA_NO_SPACES))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Visa)
    }

    @Test
    fun fromCardNumber_withInvalidVisa() {
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("1" + CardNumberFixtures.VISA_NO_SPACES))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
    }

    @Test
    fun isValidCardLengthWithBrand_whenBrandUnknown_alwaysReturnsFalse() {
        // Adding this check to ensure the input number is correct
        assertTrue(
            com.stripe.android.ui.core.elements.CardBrand.Visa.isValidCardNumberLength(CardNumberFixtures.VISA_NO_SPACES)
        )
        assertTrue(
            com.stripe.android.ui.core.elements.CardBrand.DinersClub.isValidCardNumberLength(
                CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
            )
        )
        assertTrue(
            com.stripe.android.ui.core.elements.CardBrand.DinersClub.isValidCardNumberLength(
                CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
            )
        )
        assertFalse(
            com.stripe.android.ui.core.elements.CardBrand.Unknown.isValidCardNumberLength(CardNumberFixtures.VISA_NO_SPACES)
        )
    }

    @Test
    fun isMaxCvc_whenThreeDigitsAndNotAmEx_returnsTrue() {
        assertTrue(com.stripe.android.ui.core.elements.CardBrand.Visa.isMaxCvc("123"))
        assertTrue(com.stripe.android.ui.core.elements.CardBrand.MasterCard.isMaxCvc("345"))
        assertTrue(com.stripe.android.ui.core.elements.CardBrand.JCB.isMaxCvc("678"))
        assertTrue(com.stripe.android.ui.core.elements.CardBrand.DinersClub.isMaxCvc("910"))
        assertTrue(com.stripe.android.ui.core.elements.CardBrand.Discover.isMaxCvc("234"))
        assertTrue(com.stripe.android.ui.core.elements.CardBrand.Unknown.isMaxCvc("3333"))
    }

    @Test
    fun isMaxCvc_whenThreeDigitsAndIsAmEx_returnsFalse() {
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress.isMaxCvc("123"))
    }

    @Test
    fun isMaxCvc_whenFourDigitsAndIsAmEx_returnsTrue() {
        assertTrue(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress.isMaxCvc("1234"))
    }

    @Test
    fun isMaxCvc_whenTooManyDigits_returnsFalse() {
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress.isMaxCvc("12345"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.Visa.isMaxCvc("1234"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.MasterCard.isMaxCvc("123456"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.DinersClub.isMaxCvc("1234567"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.Discover.isMaxCvc("12345678"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.JCB.isMaxCvc("123456789012345"))
    }

    @Test
    fun isMaxCvc_whenNotEnoughDigits_returnsFalse() {
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress.isMaxCvc(""))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.Visa.isMaxCvc("1"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.MasterCard.isMaxCvc("12"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.DinersClub.isMaxCvc(""))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.Discover.isMaxCvc("8"))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.JCB.isMaxCvc("1"))
    }

    @Test
    fun isMaxCvc_whenWhitespaceAndNotEnoughDigits_returnsFalse() {
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress.isMaxCvc("   "))
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.Visa.isMaxCvc("  1"))
    }

    @Test
    fun isMaxCvc_whenNull_returnsFalse() {
        assertFalse(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress.isMaxCvc(null))
    }

    @Test
    fun getMaxLengthForCardNumber_for14DigitDinersClub_shouldReturn14() {
        assertEquals(
            14,
            com.stripe.android.ui.core.elements.CardBrand.DinersClub.getMaxLengthForCardNumber(
                CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
            )
        )
    }

    @Test
    fun getMaxLengthForCardNumber_for16DigitDinersClub_shouldReturn16() {
        assertEquals(
            16,
            com.stripe.android.ui.core.elements.CardBrand.DinersClub.getMaxLengthForCardNumber(
                CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
            )
        )
    }

    @Test
    fun fromCardNumber_shouldUsePartialPatternsIfAvailable() {
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("3"))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("35"))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.JCB)
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("352"))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.JCB)
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("3527"))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("3528"))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.JCB)
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("352800"))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.JCB)
    }

    @Test
    fun fromCardNumber_withMaestroBin_shouldReturnMastercard() {
        assertThat(com.stripe.android.ui.core.elements.CardBrand.fromCardNumber("561243"))
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.MasterCard)
    }
}

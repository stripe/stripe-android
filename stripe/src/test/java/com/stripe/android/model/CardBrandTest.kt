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
        assertThat(CardBrand.fromCardNumber(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES))
            .isEqualTo(CardBrand.DinersClub)
    }

    @Test
    fun fromCardNumber_withDinersClub16() {
        assertThat(CardBrand.fromCardNumber(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES))
            .isEqualTo(CardBrand.DinersClub)
    }

    @Test
    fun fromCardNumber_withJcb() {
        assertThat(CardBrand.fromCardNumber(CardNumberFixtures.JCB_NO_SPACES))
            .isEqualTo(CardBrand.JCB)
    }

    @Test
    fun fromCardNumber_withVisa() {
        assertThat(CardBrand.fromCardNumber(CardNumberFixtures.VISA_NO_SPACES))
            .isEqualTo(CardBrand.Visa)
    }

    @Test
    fun fromCardNumber_withInvalidVisa() {
        assertThat(CardBrand.fromCardNumber("1" + CardNumberFixtures.VISA_NO_SPACES))
            .isEqualTo(CardBrand.Unknown)
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

    @Test
    fun groupNumber_withVisaDebit_returnsCorrectCardGroups() {
        assertThat(
            CardBrand.Visa.groupNumber(CardNumberFixtures.VISA_DEBIT_NO_SPACES)
        ).isEqualTo(
            arrayOf("4000", "0566", "5566", "5556")
        )
    }

    @Test
    fun groupNumber_withAmex_returnsCorrectCardGroups() {
        assertThat(
            CardBrand.AmericanExpress.groupNumber(CardNumberFixtures.AMEX_NO_SPACES)
        ).isEqualTo(
            arrayOf("3782", "822463", "10005")
        )
    }

    @Test
    fun groupNumber_withDinersClub14_returnsCorrectCardGroups() {
        assertThat(
            CardBrand.DinersClub.groupNumber(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES)
        ).isEqualTo(
            arrayOf("3622", "720627", "1667")
        )
    }

    @Test
    fun groupNumber_withDinersClub16_returnsCorrectCardGroups() {
        assertThat(
            CardBrand.DinersClub.groupNumber(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES)
        ).isEqualTo(
            arrayOf("3056", "9300", "0902", "0004")
        )
    }

    @Test
    fun groupNumber_withInvalid_returnsCorrectCardGroups() {
        assertThat(
            CardBrand.Unknown.groupNumber("1234056655665556")
        ).isEqualTo(
            arrayOf("1234", "0566", "5566", "5556")
        )
    }

    @Test
    fun groupNumber_withAmexPrefix_returnsPrefixGroups() {
        assertThat(
            CardBrand.AmericanExpress.groupNumber(
                CardNumberFixtures.AMEX_NO_SPACES.take(2)
            )
        ).isEqualTo(
            arrayOf("37", null, null)
        )

        assertThat(
            CardBrand.AmericanExpress.groupNumber(
                CardNumberFixtures.AMEX_NO_SPACES.take(5)
            )
        ).isEqualTo(
            arrayOf("3782", "8", null)
        )

        assertThat(
            CardBrand.AmericanExpress.groupNumber(
                CardNumberFixtures.AMEX_NO_SPACES.take(11)
            )
        ).isEqualTo(
            arrayOf("3782", "822463", "1")
        )
    }

    @Test
    fun groupNumber_withVisaPrefix_returnsCorrectGroups() {
        assertThat(
            CardBrand.Visa.groupNumber(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(2)
            )
        ).isEqualTo(
            arrayOf("40", null, null, null)
        )

        assertThat(
            CardBrand.Visa.groupNumber(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(5)
            )
        ).isEqualTo(
            arrayOf("4000", "0", null, null)
        )

        assertThat(
            CardBrand.Visa.groupNumber(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(9)
            )
        ).isEqualTo(
            arrayOf("4000", "0566", "5", null)
        )

        assertThat(
            CardBrand.Visa.groupNumber(
                CardNumberFixtures.VISA_DEBIT_NO_SPACES.take(15)
            )
        ).isEqualTo(
            arrayOf("4000", "0566", "5566", "555")
        )
    }

    @Test
    fun groupNumber_forLongInputs_doesNotCrash() {
        assertThat(
            CardBrand.Visa.groupNumber("1234567890123456789")
        ).hasLength(4)
    }

    @Test
    fun formatNumber_shouldReturnExpectedValue() {
        assertThat(CardBrand.AmericanExpress.formatNumber(CardNumberFixtures.AMEX_NO_SPACES))
            .isEqualTo(CardNumberFixtures.AMEX_WITH_SPACES)
        assertThat(CardBrand.Visa.formatNumber(CardNumberFixtures.VISA_NO_SPACES))
            .isEqualTo(CardNumberFixtures.VISA_WITH_SPACES)
        assertThat(CardBrand.Visa.formatNumber(CardNumberFixtures.VISA_DEBIT_NO_SPACES))
            .isEqualTo(CardNumberFixtures.VISA_DEBIT_WITH_SPACES)
        assertThat(CardBrand.DinersClub.formatNumber(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES))
            .isEqualTo(CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES)
        assertThat(CardBrand.DinersClub.formatNumber(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES))
            .isEqualTo(CardNumberFixtures.DINERS_CLUB_16_WITH_SPACES)
        assertThat(CardBrand.MasterCard.formatNumber(CardNumberFixtures.MASTERCARD_NO_SPACES))
            .isEqualTo(CardNumberFixtures.MASTERCARD_WITH_SPACES)
        assertThat(CardBrand.JCB.formatNumber(CardNumberFixtures.JCB_NO_SPACES))
            .isEqualTo(CardNumberFixtures.JCB_WITH_SPACES)
        assertThat(CardBrand.Discover.formatNumber(CardNumberFixtures.DISCOVER_NO_SPACES))
            .isEqualTo(CardNumberFixtures.DISCOVER_WITH_SPACES)
        assertThat(CardBrand.UnionPay.formatNumber(CardNumberFixtures.UNIONPAY_NO_SPACES))
            .isEqualTo(CardNumberFixtures.UNIONPAY_WITH_SPACES)
    }

    @Test
    fun defaultMaxLengthWithSpaces_shouldReturnExpectedValue() {
        assertEquals(19, CardBrand.Visa.defaultMaxLengthWithSpaces)
    }

    @Test
    fun getMaxLengthForCardNumber_for14DigitDinersClub_shouldReturn14() {
        assertEquals(
            14,
            CardBrand.DinersClub.getMaxLengthForCardNumber(
                CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
            )
        )
    }

    @Test
    fun getMaxLengthForCardNumber_for16DigitDinersClub_shouldReturn16() {
        assertEquals(
            16,
            CardBrand.DinersClub.getMaxLengthForCardNumber(
                CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
            )
        )
    }
}

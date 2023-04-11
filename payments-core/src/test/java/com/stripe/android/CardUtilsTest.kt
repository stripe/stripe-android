package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import kotlin.test.Test

/**
 * Test class for [CardUtils].
 */
class CardUtilsTest {

    @Test
    fun getPossibleCardBrand_withEmptyCard_returnsUnknown() {
        assertThat(CardUtils.getPossibleCardBrand("   ")).isEqualTo(CardBrand.Unknown)
    }

    @Test
    fun getPossibleCardBrand_withNullCardNumber_returnsUnknown() {
        assertThat(CardUtils.getPossibleCardBrand(null)).isEqualTo(CardBrand.Unknown)
    }

    @Test
    fun getPossibleCardBrand_withVisaPrefix_returnsVisa() {
        assertThat(CardUtils.getPossibleCardBrand("4899 99")).isEqualTo(CardBrand.Visa)
        assertThat(CardUtils.getPossibleCardBrand("4")).isEqualTo(CardBrand.Visa)
    }

    @Test
    fun getPossibleCardBrand_withAmexPrefix_returnsAmex() {
        assertThat(CardUtils.getPossibleCardBrand("345")).isEqualTo(CardBrand.AmericanExpress)
        assertThat(CardUtils.getPossibleCardBrand("37999999999")).isEqualTo(CardBrand.AmericanExpress)
    }

    @Test
    fun getPossibleCardBrand_withJCBPrefix_returnsJCB() {
        assertThat(CardUtils.getPossibleCardBrand("3535 3535")).isEqualTo(CardBrand.JCB)
    }

    @Test
    fun getPossibleCardBrand_withMasterCardPrefix_returnsMasterCard() {
        assertThat(CardUtils.getPossibleCardBrand("2222 452")).isEqualTo(CardBrand.MasterCard)
        assertThat(CardUtils.getPossibleCardBrand("5050")).isEqualTo(CardBrand.MasterCard)
    }

    @Test
    fun getPossibleCardBrand_withDinersClub16Prefix_returnsDinersClub() {
        assertThat(CardUtils.getPossibleCardBrand("303922 2234")).isEqualTo(CardBrand.DinersClub)
    }

    @Test
    fun getPossibleCardBrand_withDinersClub14Prefix_returnsDinersClub() {
        assertThat(CardUtils.getPossibleCardBrand("36778 9098")).isEqualTo(CardBrand.DinersClub)
    }

    @Test
    fun getPossibleCardBrand_withDiscoverPrefix_returnsDiscover() {
        assertThat(CardUtils.getPossibleCardBrand("60355")).isEqualTo(CardBrand.Discover)
        assertThat(CardUtils.getPossibleCardBrand("6433 8 90923")).isEqualTo(CardBrand.Discover)
        // This one has too many numbers on purpose. Checking for length is not part of the
        // function under test.
        assertThat(CardUtils.getPossibleCardBrand("6523452309209340293423")).isEqualTo(CardBrand.Discover)
    }

    @Test
    fun getPossibleCardBrand_withUnionPayPrefix_returnsUnionPay() {
        assertThat(CardUtils.getPossibleCardBrand("62")).isEqualTo(CardBrand.UnionPay)
    }

    @Test
    fun getPossibleCardBrand_withNonsenseNumber_returnsUnknown() {
        assertThat(CardUtils.getPossibleCardBrand("1234567890123456")).isEqualTo(CardBrand.Unknown)
        assertThat(CardUtils.getPossibleCardBrand("9999 9999 9999 9999")).isEqualTo(CardBrand.Unknown)
        assertThat(CardUtils.getPossibleCardBrand("3")).isEqualTo(CardBrand.Unknown)
    }

    @Test
    fun isValidLuhnNumber_whenValidVisaNumber_returnsTrue() {
        assertThat(CardUtils.isValidLuhnNumber(CardNumberFixtures.VISA_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidJCBNumber_returnsTrue() {
        assertThat(CardUtils.isValidLuhnNumber("3530111333300000")).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidDiscover_returnsTrue() {
        assertThat(CardUtils.isValidLuhnNumber(CardNumberFixtures.DISCOVER_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidDinersClub_returnsTrue() {
        assertThat(CardUtils.isValidLuhnNumber("30569309025904")).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidMasterCard_returnsTrue() {
        assertThat(CardUtils.isValidLuhnNumber(CardNumberFixtures.MASTERCARD_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidAmEx_returnsTrue() {
        assertThat(CardUtils.isValidLuhnNumber(CardNumberFixtures.AMEX_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLunhNumber_whenNumberIsInvalid_returnsFalse() {
        assertThat(CardUtils.isValidLuhnNumber("4242424242424243")).isFalse()
    }

    @Test
    fun isValidLuhnNumber_whenInputIsNull_returnsFalse() {
        assertThat(CardUtils.isValidLuhnNumber(null)).isFalse()
    }

    @Test
    fun isValidLuhnNumber_whenInputIsNotNumeric_returnsFalse() {
        assertThat(CardUtils.isValidLuhnNumber("abcdefg")).isFalse()
        // Note: it is not the job of this function to de-space the card number, nor de-hyphen it
        assertThat(CardUtils.isValidLuhnNumber("4242 4242 4242 4242")).isFalse()
        assertThat(CardUtils.isValidLuhnNumber("4242-4242-4242-4242")).isFalse()
    }
}

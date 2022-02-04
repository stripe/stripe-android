package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.elements.CardBrand
import kotlin.test.Test

/**
 * Test class for [CardUtils].
 */
class CardUtilsTest {

    @Test
    fun getPossibleCardBrand_withEmptyCard_returnsUnknown() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("   ")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
    }

    @Test
    fun getPossibleCardBrand_withNullCardNumber_returnsUnknown() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand(null)).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
    }

    @Test
    fun getPossibleCardBrand_withVisaPrefix_returnsVisa() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("4899 99")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Visa)
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("4")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Visa)
    }

    @Test
    fun getPossibleCardBrand_withAmexPrefix_returnsAmex() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("345")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress)
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("37999999999")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.AmericanExpress)
    }

    @Test
    fun getPossibleCardBrand_withJCBPrefix_returnsJCB() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("3535 3535")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.JCB)
    }

    @Test
    fun getPossibleCardBrand_withMasterCardPrefix_returnsMasterCard() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("2222 452")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.MasterCard)
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("5050")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.MasterCard)
    }

    @Test
    fun getPossibleCardBrand_withDinersClub16Prefix_returnsDinersClub() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("303922 2234")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.DinersClub)
    }

    @Test
    fun getPossibleCardBrand_withDinersClub14Prefix_returnsDinersClub() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("36778 9098")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.DinersClub)
    }

    @Test
    fun getPossibleCardBrand_withDiscoverPrefix_returnsDiscover() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("60355")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Discover)
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("6433 8 90923")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Discover)
        // This one has too many numbers on purpose. Checking for length is not part of the
        // function under test.
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("6523452309209340293423")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Discover)
    }

    @Test
    fun getPossibleCardBrand_withUnionPayPrefix_returnsUnionPay() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("62")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.UnionPay)
    }

    @Test
    fun getPossibleCardBrand_withNonsenseNumber_returnsUnknown() {
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("1234567890123456")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("9999 9999 9999 9999")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
        assertThat(com.stripe.android.CardUtils.getPossibleCardBrand("3")).isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Unknown)
    }

    @Test
    fun isValidLuhnNumber_whenValidVisaNumber_returnsTrue() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber(CardNumberFixtures.VISA_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidJCBNumber_returnsTrue() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber("3530111333300000")).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidDiscover_returnsTrue() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber(CardNumberFixtures.DISCOVER_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidDinersClub_returnsTrue() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber("30569309025904")).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidMasterCard_returnsTrue() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber(CardNumberFixtures.MASTERCARD_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLuhnNumber_whenValidAmEx_returnsTrue() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber(CardNumberFixtures.AMEX_NO_SPACES)).isTrue()
    }

    @Test
    fun isValidLunhNumber_whenNumberIsInvalid_returnsFalse() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber("4242424242424243")).isFalse()
    }

    @Test
    fun isValidLuhnNumber_whenInputIsNull_returnsFalse() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber(null)).isFalse()
    }

    @Test
    fun isValidLuhnNumber_whenInputIsNotNumeric_returnsFalse() {
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber("abcdefg")).isFalse()
        // Note: it is not the job of this function to de-space the card number, nor de-hyphen it
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber("4242 4242 4242 4242")).isFalse()
        assertThat(com.stripe.android.CardUtils.isValidLuhnNumber("4242-4242-4242-4242")).isFalse()
    }
}

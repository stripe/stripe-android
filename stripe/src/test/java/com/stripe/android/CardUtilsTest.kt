package com.stripe.android

import com.stripe.android.model.CardBrand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for [CardUtils].
 */
class CardUtilsTest {

    @Test
    fun getPossibleCardBrand_withEmptyCard_returnsUnknown() {
        assertEquals(CardBrand.Unknown, CardUtils.getPossibleCardBrand("   "))
    }

    @Test
    fun getPossibleCardBrand_withNullCardNumber_returnsUnknown() {
        assertEquals(CardBrand.Unknown, CardUtils.getPossibleCardBrand(null))
    }

    @Test
    fun getPossibleCardBrand_withVisaPrefix_returnsVisa() {
        assertEquals(CardBrand.Visa, CardUtils.getPossibleCardBrand("4899 99"))
        assertEquals(CardBrand.Visa, CardUtils.getPossibleCardBrand("4"))
    }

    @Test
    fun getPossibleCardBrand_withAmexPrefix_returnsAmex() {
        assertEquals(CardBrand.AmericanExpress, CardUtils.getPossibleCardBrand("345"))
        assertEquals(CardBrand.AmericanExpress, CardUtils.getPossibleCardBrand("37999999999"))
    }

    @Test
    fun getPossibleCardBrand_withJCBPrefix_returnsJCB() {
        assertEquals(CardBrand.JCB, CardUtils.getPossibleCardBrand("3535 3535"))
    }

    @Test
    fun getPossibleCardBrand_withMasterCardPrefix_returnsMasterCard() {
        assertEquals(CardBrand.MasterCard, CardUtils.getPossibleCardBrand("2222 452"))
        assertEquals(CardBrand.MasterCard, CardUtils.getPossibleCardBrand("5050"))
    }

    @Test
    fun getPossibleCardBrand_withDinersClub16Prefix_returnsDinersClub() {
        assertEquals(CardBrand.DinersClub, CardUtils.getPossibleCardBrand("303922 2234"))
    }

    @Test
    fun getPossibleCardBrand_withDinersClub14Prefix_returnsDinersClub() {
        assertEquals(CardBrand.DinersClub, CardUtils.getPossibleCardBrand("36778 9098"))
    }

    @Test
    fun getPossibleCardBrand_withDiscoverPrefix_returnsDiscover() {
        assertEquals(CardBrand.Discover, CardUtils.getPossibleCardBrand("60355"))
        assertEquals(CardBrand.Discover, CardUtils.getPossibleCardBrand("6433 8 90923"))
        // This one has too many numbers on purpose. Checking for length is not part of the
        // function under test.
        assertEquals(CardBrand.Discover, CardUtils.getPossibleCardBrand("6523452309209340293423"))
    }

    @Test
    fun getPossibleCardBrand_withUnionPayPrefix_returnsUnionPay() {
        assertEquals(CardBrand.UnionPay, CardUtils.getPossibleCardBrand("62"))
    }

    @Test
    fun getPossibleCardBrand_withNonsenseNumber_returnsUnknown() {
        assertEquals(CardBrand.Unknown, CardUtils.getPossibleCardBrand("1234567890123456"))
        assertEquals(CardBrand.Unknown, CardUtils.getPossibleCardBrand("9999 9999 9999 9999"))
        assertEquals(CardBrand.Unknown, CardUtils.getPossibleCardBrand("3"))
    }

    @Test
    fun isValidCardLength_whenValidVisaNumber_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength(CardNumberFixtures.VISA_NO_SPACES))
    }

    @Test
    fun isValidCardLength_whenValidJCBNumber_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("3530111333300000"))
    }

    @Test
    fun isValidCardLength_whenValidDiscover_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength(CardNumberFixtures.DISCOVER_NO_SPACES))
    }

    @Test
    fun isValidCardLength_whenValidDinersClub16_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES))
    }

    @Test
    fun isValidCardLength_whenValidMasterCard_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength(CardNumberFixtures.MASTERCARD_NO_SPACES))
    }

    @Test
    fun isValidCardLength_whenValidAmEx_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength(CardNumberFixtures.AMEX_NO_SPACES))
    }

    @Test
    fun isValidCardLength_whenValidUnionPay_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength(CardNumberFixtures.UNIONPAY_NO_SPACES))
    }

    @Test
    fun isValidCardLength_whenNull_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength(null))
    }

    @Test
    fun isValidCardLength_whenVisaStyleNumberButDinersClubLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("42424242424242"))
    }

    @Test
    fun isValidCardLength_whenVisaStyleNumberButAmExLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("424242424242424"))
    }

    @Test
    fun isValidCardLength_whenAmExStyleNumberButVisaLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("3782822463100050"))
    }

    @Test
    fun isValidCardLength_whenAmExStyleNumberButDinersClubLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("37828224631000"))
    }

    @Test
    fun isValidCardLength_whenDinersClubStyleNumberStyleNumberButAmexLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("305693090259040"))
    }

    @Test
    fun isValidLuhnNumber_whenValidVisaNumber_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber(CardNumberFixtures.VISA_NO_SPACES))
    }

    @Test
    fun isValidLuhnNumber_whenValidJCBNumber_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("3530111333300000"))
    }

    @Test
    fun isValidLuhnNumber_whenValidDiscover_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber(CardNumberFixtures.DISCOVER_NO_SPACES))
    }

    @Test
    fun isValidLuhnNumber_whenValidDinersClub_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("30569309025904"))
    }

    @Test
    fun isValidLuhnNumber_whenValidMasterCard_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber(CardNumberFixtures.MASTERCARD_NO_SPACES))
    }

    @Test
    fun isValidLuhnNumber_whenValidAmEx_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber(CardNumberFixtures.AMEX_NO_SPACES))
    }

    @Test
    fun isValidLunhNumber_whenNumberIsInvalid_returnsFalse() {
        assertFalse(CardUtils.isValidLuhnNumber("4242424242424243"))
    }

    @Test
    fun isValidLuhnNumber_whenInputIsNull_returnsFalse() {
        assertFalse(CardUtils.isValidLuhnNumber(null))
    }

    @Test
    fun isValidLuhnNumber_whenInputIsNotNumeric_returnsFalse() {
        assertFalse(CardUtils.isValidLuhnNumber("abcdefg"))
        // Note: it is not the job of this function to de-space the card number, nor de-hyphen it
        assertFalse(CardUtils.isValidLuhnNumber("4242 4242 4242 4242"))
        assertFalse(CardUtils.isValidLuhnNumber("4242-4242-4242-4242"))
    }
}

package com.stripe.android

import com.stripe.android.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for [CardUtils].
 */
class CardUtilsTest {

    @Test
    fun getPossibleCardType_withEmptyCard_returnsUnknown() {
        assertEquals(Card.CardBrand.UNKNOWN, CardUtils.getPossibleCardType("   "))
    }

    @Test
    fun getPossibleCardType_withNullCardNumber_returnsUnknown() {
        assertEquals(Card.CardBrand.UNKNOWN, CardUtils.getPossibleCardType(null))
    }

    @Test
    fun getPossibleCardType_withVisaPrefix_returnsVisa() {
        assertEquals(Card.CardBrand.VISA, CardUtils.getPossibleCardType("4899 99"))
        assertEquals(Card.CardBrand.VISA, CardUtils.getPossibleCardType("4"))
    }

    @Test
    fun getPossibleCardType_withAmexPrefix_returnsAmex() {
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, CardUtils.getPossibleCardType("345"))
        assertEquals(Card.CardBrand.AMERICAN_EXPRESS, CardUtils.getPossibleCardType("37999999999"))
    }

    @Test
    fun getPossibleCardType_withJCBPrefix_returnsJCB() {
        assertEquals(Card.CardBrand.JCB, CardUtils.getPossibleCardType("3535 3535"))
    }

    @Test
    fun getPossibleCardType_withMasterCardPrefix_returnsMasterCard() {
        assertEquals(Card.CardBrand.MASTERCARD, CardUtils.getPossibleCardType("2222 452"))
        assertEquals(Card.CardBrand.MASTERCARD, CardUtils.getPossibleCardType("5050"))
    }

    @Test
    fun getPossibleCardType_withDinersClubPrefix_returnsDinersClub() {
        assertEquals(Card.CardBrand.DINERS_CLUB, CardUtils.getPossibleCardType("303922 2234"))
        assertEquals(Card.CardBrand.DINERS_CLUB, CardUtils.getPossibleCardType("36778 9098"))
    }

    @Test
    fun getPossibleCardType_withDiscoverPrefix_returnsDiscover() {
        assertEquals(Card.CardBrand.DISCOVER, CardUtils.getPossibleCardType("60355"))
        assertEquals(Card.CardBrand.DISCOVER, CardUtils.getPossibleCardType("6433 8 90923"))
        // This one has too many numbers on purpose. Checking for length is not part of the
        // function under test.
        assertEquals(Card.CardBrand.DISCOVER, CardUtils.getPossibleCardType("6523452309209340293423"))
    }

    @Test
    fun getPossibleCardType_withUnionPayPrefix_returnsUnionPay() {
        assertEquals(Card.CardBrand.UNIONPAY, CardUtils.getPossibleCardType("62"))
    }

    @Test
    fun getPossibleCardType_withNonsenseNumber_returnsUnknown() {
        assertEquals(Card.CardBrand.UNKNOWN, CardUtils.getPossibleCardType("1234567890123456"))
        assertEquals(Card.CardBrand.UNKNOWN, CardUtils.getPossibleCardType("9999 9999 9999 9999"))
        assertEquals(Card.CardBrand.UNKNOWN, CardUtils.getPossibleCardType("3"))
    }

    @Test
    fun isValidCardLength_whenValidVisaNumber_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("4242424242424242"))
    }

    @Test
    fun isValidCardLength_whenValidJCBNumber_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("3530111333300000"))
    }

    @Test
    fun isValidCardLength_whenValidDiscover_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("6011000990139424"))
    }

    @Test
    fun isValidCardLength_whenValidDinersClub_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("30569309025904"))
    }

    @Test
    fun isValidCardLength_whenValidMasterCard_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("5555555555554444"))
    }

    @Test
    fun isValidCardLength_whenValidAmEx_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("378282246310005"))
    }

    @Test
    fun isValidCardLength_whenValidUnionPay_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("6200000000000005"))
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
    fun isValidCardLength_whenDinersClubStyleNumberButVisaLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("3056930902590400"))
    }

    @Test
    fun isValidCardLength_whenDinersClubStyleNumberStyleNumberButAmexLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("305693090259040"))
    }

    @Test
    fun isValidCardLengthWithBrand_whenBrandUnknown_alwaysReturnsFalse() {
        val validVisa = "4242424242424242"
        // Adding this check to ensure the input number is correct
        assertTrue(CardUtils.isValidCardLength(validVisa))
        assertFalse(CardUtils.isValidCardLength(validVisa, Card.CardBrand.UNKNOWN))
    }

    @Test
    fun isValidLuhnNumber_whenValidVisaNumber_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("4242424242424242"))
    }

    @Test
    fun isValidLuhnNumber_whenValidJCBNumber_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("3530111333300000"))
    }

    @Test
    fun isValidLuhnNumber_whenValidDiscover_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("6011000990139424"))
    }

    @Test
    fun isValidLuhnNumber_whenValidDinersClub_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("30569309025904"))
    }

    @Test
    fun isValidLuhnNumber_whenValidMasterCard_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("5555555555554444"))
    }

    @Test
    fun isValidLuhnNumber_whenValidAmEx_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("378282246310005"))
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

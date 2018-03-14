package com.stripe.android;

import com.stripe.android.model.Card;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CardUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CardUtilsTest {

    @Test
    public void getPossibleCardType_withEmptyCard_returnsUnknown() {
        assertEquals(Card.UNKNOWN, CardUtils.getPossibleCardType("   "));
    }

    @Test
    public void getPossibleCardType_withNullCardNumber_returnsUnknown() {
        assertEquals(Card.UNKNOWN, CardUtils.getPossibleCardType(null));
    }

    @Test
    public void getPossibleCardType_withVisaPrefix_returnsVisa() {
        assertEquals(Card.VISA, CardUtils.getPossibleCardType("4899 99"));
        assertEquals(Card.VISA, CardUtils.getPossibleCardType("4"));
    }

    @Test
    public void getPossibleCardType_withAmexPrefix_returnsAmex() {
        assertEquals(Card.AMERICAN_EXPRESS, CardUtils.getPossibleCardType("345"));
        assertEquals(Card.AMERICAN_EXPRESS, CardUtils.getPossibleCardType("37999999999"));
    }

    @Test
    public void getPossibleCardType_withJCBPrefix_returnsJCB() {
        assertEquals(Card.JCB, CardUtils.getPossibleCardType("3535 3535"));
    }

    @Test
    public void getPossibleCardType_withMasterCardPrefix_returnsMasterCard() {
        assertEquals(Card.MASTERCARD, CardUtils.getPossibleCardType("2222 452"));
        assertEquals(Card.MASTERCARD, CardUtils.getPossibleCardType("5050"));
    }

    @Test
    public void getPossibleCardType_withDinersClubPrefix_returnsDinersClub() {
        assertEquals(Card.DINERS_CLUB, CardUtils.getPossibleCardType("303922 2234"));
        assertEquals(Card.DINERS_CLUB, CardUtils.getPossibleCardType("36778 9098"));
    }

    @Test
    public void getPossibleCardType_withDiscoverPrefix_returnsDiscover() {
        assertEquals(Card.DISCOVER, CardUtils.getPossibleCardType("60355"));
        assertEquals(Card.DISCOVER, CardUtils.getPossibleCardType("6433 8 90923"));
        // This one has too many numbers on purpose. Checking for length is not part of the
        // function under test.
        assertEquals(Card.DISCOVER, CardUtils.getPossibleCardType("6523452309209340293423"));
    }

    @Test
    public void getPossibleCardType_withUnionPayPrefix_returnsUnionPay() {
        assertEquals(Card.UNIONPAY, CardUtils.getPossibleCardType("62"));
    }

    @Test
    public void getPossibleCardType_withNonsenseNumber_returnsUnknown() {
        assertEquals(Card.UNKNOWN, CardUtils.getPossibleCardType("1234567890123456"));
        assertEquals(Card.UNKNOWN, CardUtils.getPossibleCardType("9999 9999 9999 9999"));
        assertEquals(Card.UNKNOWN, CardUtils.getPossibleCardType("3"));
    }

    @Test
    public void isValidCardLength_whenValidVisaNumber_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("4242424242424242"));
    }

    @Test
    public void isValidCardLength_whenValidJCBNumber_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("3530111333300000"));
    }

    @Test
    public void isValidCardLength_whenValidDiscover_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("6011000990139424"));
    }

    @Test
    public void isValidCardLength_whenValidDinersClub_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("30569309025904"));
    }

    @Test
    public void isValidCardLength_whenValidMasterCard_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("5555555555554444"));
    }

    @Test
    public void isValidCardLength_whenValidAmEx_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("378282246310005"));
    }

    @Test
    public void isValidCardLength_whenValidUnionPay_returnsTrue() {
        assertTrue(CardUtils.isValidCardLength("6200000000000005"));
    }

    @Test
    public void isValidCardLength_whenNull_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength(null));
    }

    @Test
    public void isValidCardLength_whenVisaStyleNumberButDinersClubLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("42424242424242"));
    }

    @Test
    public void isValidCardLength_whenVisaStyleNumberButAmExLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("424242424242424"));
    }

    @Test
    public void isValidCardLength_whenAmExStyleNumberButVisaLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("3782822463100050"));
    }

    @Test
    public void isValidCardLength_whenAmExStyleNumberButDinersClubLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("37828224631000"));
    }

    @Test
    public void isValidCardLength_whenDinersClubStyleNumberButVisaLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("3056930902590400"));
    }

    @Test
    public void isValidCardLength_whenDinersClubStyleNumberStyleNumberButAmexLength_returnsFalse() {
        assertFalse(CardUtils.isValidCardLength("305693090259040"));
    }

    @Test
    public void isValidCardLengthWithBrand_whenBrandUnknown_alwaysReturnsFalse() {
        String validVisa = "4242424242424242";
        // Adding this check to ensure the input number is correct
        assertTrue(CardUtils.isValidCardLength(validVisa));
        assertFalse(CardUtils.isValidCardLength(validVisa, Card.UNKNOWN));
    }

    @Test
    public void isValidLuhnNumber_whenValidVisaNumber_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("4242424242424242"));
    }

    @Test
    public void isValidLuhnNumber_whenValidJCBNumber_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("3530111333300000"));
    }

    @Test
    public void isValidLuhnNumber_whenValidDiscover_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("6011000990139424"));
    }

    @Test
    public void isValidLuhnNumber_whenValidDinersClub_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("30569309025904"));
    }

    @Test
    public void isValidLuhnNumber_whenValidMasterCard_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("5555555555554444"));
    }

    @Test
    public void isValidLuhnNumber_whenValidAmEx_returnsTrue() {
        assertTrue(CardUtils.isValidLuhnNumber("378282246310005"));
    }

    @Test
    public void isValidLunhNumber_whenNumberIsInvalid_returnsFalse() {
        assertFalse(CardUtils.isValidLuhnNumber("4242424242424243"));
    }

    @Test
    public void isValidLuhnNumber_whenInputIsNull_returnsFalse() {
        assertFalse(CardUtils.isValidLuhnNumber(null));
    }

    @Test
    public void isValidLuhnNumber_whenInputIsNotNumeric_returnsFalse() {
        assertFalse(CardUtils.isValidLuhnNumber("abcdefg"));
        // Note: it is not the job of this function to de-space the card number, nor de-hyphen it
        assertFalse(CardUtils.isValidLuhnNumber("4242 4242 4242 4242"));
        assertFalse(CardUtils.isValidLuhnNumber("4242-4242-4242-4242"));
    }
}

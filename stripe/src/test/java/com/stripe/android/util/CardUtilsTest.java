package com.stripe.android.util;

import com.stripe.android.model.Card;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        assertEquals(Card.DISCOVER, CardUtils.getPossibleCardType("62"));
        assertEquals(Card.DISCOVER, CardUtils.getPossibleCardType("6433 8 90923"));
        // This one has too many numbers on purpose. Checking for length is not part of the
        // function under test.
        assertEquals(Card.DISCOVER, CardUtils.getPossibleCardType("6523452309209340293423"));
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


    @Test
    public void separateCardNumberGroups_withVisa_returnsCorrectCardGroups() {
        String testCardNumber = "4000056655665556";
        String[] groups = CardUtils.separateCardNumberGroups(testCardNumber, Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5566", groups[2]);
        assertEquals("5556", groups[3]);
    }

    @Test
    public void separateCardNumberGroups_withAmex_returnsCorrectCardGroups() {
        String testCardNumber = "378282246310005";
        String[] groups =
                CardUtils.separateCardNumberGroups(testCardNumber, Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("822463", groups[1]);
        assertEquals("10005", groups[2]);
    }

    @Test
    public void separateCardNumberGroups_withDinersClub_returnsCorrectCardGroups() {
        String testCardNumber = "38520000023237";
        String[] groups =
                CardUtils.separateCardNumberGroups(testCardNumber, Card.DINERS_CLUB);
        assertEquals(4, groups.length);
        assertEquals("3852", groups[0]);
        assertEquals("0000", groups[1]);
        assertEquals("0232", groups[2]);
        assertEquals("37", groups[3]);
    }

    @Test
    public void separateCardNumberGroups_withInvalid_returnsCorrectCardGroups() {
        String testCardNumber = "1234056655665556";
        String[] groups = CardUtils.separateCardNumberGroups(testCardNumber, Card.UNKNOWN);
        assertEquals(4, groups.length);
        assertEquals("1234", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5566", groups[2]);
        assertEquals("5556", groups[3]);
    }

    @Test
    public void separateCardNumberGroups_withAmexPrefix_returnsPrefixGroups() {
        String testCardNumber = "378282246310005";
        String[] groups = CardUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 2), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("37", groups[0]);
        assertNull(groups[1]);
        assertNull(groups[2]);

        groups = CardUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 5), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("8", groups[1]);
        assertNull(groups[2]);

        groups = CardUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 11), Card.AMERICAN_EXPRESS);
        assertEquals(3, groups.length);
        assertEquals("3782", groups[0]);
        assertEquals("822463", groups[1]);
        assertEquals("1", groups[2]);
    }

    @Test
    public void separateCardNumberGroups_withVisaPrefix_returnsCorrectGroups() {
        String testCardNumber = "4000056655665556";
        String[] groups = CardUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 2), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("40", groups[0]);
        assertNull(groups[1]);
        assertNull(groups[2]);
        assertNull(groups[3]);

        groups = CardUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 5), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0", groups[1]);
        assertNull(groups[2]);
        assertNull(groups[3]);

        groups = CardUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 9), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5", groups[2]);
        assertNull(groups[3]);

        groups = CardUtils.separateCardNumberGroups(
                testCardNumber.substring(0, 15), Card.VISA);
        assertEquals(4, groups.length);
        assertEquals("4000", groups[0]);
        assertEquals("0566", groups[1]);
        assertEquals("5566", groups[2]);
        assertEquals("555", groups[3]);
    }
}

package com.stripe.android.test;

import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.stripe.android.model.Card;
import com.stripe.android.time.FrozenClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CardTest {
    private static final int YEAR_IN_FUTURE = 2000;

    @Before
    public void setup() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1997);
        cal.set(Calendar.MONTH, Calendar.AUGUST);
        cal.set(Calendar.DAY_OF_MONTH, 29);
        FrozenClock.freeze(cal);
    }

    @After
    public void teardown() {
       FrozenClock.unfreeze();
    }

    @Test
    public void canInitializeWithMinimalArguments() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateNumber());
    }

    @Test
    public void testTypeReturnsCorrectlyForAmexCard() {
        Card card = new Card("3412123412341234", null, null, null);
        assertEquals("American Express", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForDiscoverCard() {
        Card card = new Card("6452123412341234", null, null, null);
        assertEquals("Discover", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForJCBCard() {
        Card card = new Card("3512123412341234", null, null, null);
        assertEquals("JCB", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForDinersClubCard() {
        Card card = new Card("3612123412341234", null, null, null);
        assertEquals("Diners Club", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForVisaCard() {
        Card card = new Card("4112123412341234", null, null, null);
        assertEquals("Visa", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForMasterCard() {
        Card card = new Card("5112123412341234", null, null, null);
        assertEquals("MasterCard", card.getType());
    }

    @Test
    public void shouldPassValidateNumberIfLuhnNumber() {
        Card card = new Card("4242-4242-4242-4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfNotLuhnNumber() {
        Card card = new Card("4242-4242-4242-4241", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberIfLuhnNumberAmex() {
        Card card = new Card("378282246310005", null, null, null);
        assertEquals("American Express", card.getType());
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfNull() {
        Card card = new Card(null, null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfBlank() {
        Card card = new Card("", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfJustSpaces() {
        Card card = new Card("    ", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfTooShort() {
        Card card = new Card("0", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfContainsLetters() {
        Card card = new Card("424242424242a4242", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfTooLong() {
        Card card = new Card("4242 4242 4242 4242 6", null, null, null);
        assertEquals("Visa", card.getType());
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumber() {
        Card card = new Card("4242424242424242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberSpaces() {
        Card card = new Card("4242 4242 4242 4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberDashes() {
        Card card = new Card("4242-4242-4242-4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberWithMixedSeparators() {
        Card card = new Card("4242-4   242 424-24 242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfWithDot() {
        Card card = new Card("4242.4242.4242.4242", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNull() {
        Card card = new Card(null, null, null, null);
        assertFalse(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNullMonth() {
        Card card = new Card(null, null, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfZeroMonth() {
        Card card = new Card(null, 0, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNegativeMonth() {
        Card card = new Card(null, -1, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfMonthToLarge() {
        Card card = new Card(null, 13, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNullYear() {
        Card card = new Card(null, 1, null, null);
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfZeroYear() {
        Card card = new Card(null, 12, 0, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNegativeYear() {
        Card card = new Card(null, 12, -1, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateForDecemberOfThisYear() {
        Card card = new Card(null, 12, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonth() {
        Card card = new Card(null, 8, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonthTwoDigitYear() {
        Card card = new Card(null, 8, 97, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfLastMonth() {
        Card card = new Card(null, 7, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfNextMonth() {
        Card card = new Card(null, 9, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateForJanuary00() {
        Card card = new Card(null, 1, 0, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateForDecember99() {
        Card card = new Card(null, 12, 99, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateCVCIfNull() {
        Card card = new Card(null, null, null, null);
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfBlank() {
        Card card = new Card(null, null, null, "");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfUnknownTypeAndLength2() {
        Card card = new Card(null, null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfUnknownTypeAndLength3() {
        Card card = new Card(null, null, null, "123");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfUnknownTypeAndLength4() {
        Card card = new Card(null, null, null, "1234");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfUnknownTypeAndLength5() {
        Card card = new Card(null, null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength2() {
        Card card = new Card("4242 4242 4242 4242", null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfVisaAndLength3() {
        Card card = new Card("4242 4242 4242 4242", null, null, "123");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength4() {
        Card card = new Card("4242 4242 4242 4242", null, null, "1234");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength5() {
        Card card = new Card("4242 4242 4242 4242", null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndNotNumeric() {
        Card card = new Card("4242 4242 4242 4242", null, null, "12a");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfAmexAndLength2() {
        Card card = new Card("378282246310005", null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndLength3() {
        Card card = new Card("378282246310005", null, null, "123");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfAmexAndLength4() {
        Card card = new Card("378282246310005", null, null, "1234");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndLength5() {
        Card card = new Card("378282246310005", null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndNotNumeric() {
        Card card = new Card("378282246310005", null, null, "123d");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardIfNotLuhnNumber() {
        Card card = new Card("4242-4242-4242-4241", 12, 2050, "123");
        assertFalse(card.validateCard());
        assertFalse(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardInvalidMonth() {
        Card card = new Card("4242-4242-4242-4242", 13, 2050, "123");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertFalse(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardInvalidYear() {
        Card card = new Card("4242-4242-4242-4242", 1, 1990, "123");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertFalse(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardWithNullCVC() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, null);
        assertTrue(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardVisa() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithShortCVC() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, "12");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithLongCVC() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, "1234");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithBadCVC() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, "bad");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardAmex() {
        Card card = new Card("378282246310005", 12, 2050, "1234");
        assertTrue(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardAmexWithNullCVC() {
        Card card = new Card("378282246310005", 12, 2050, null);
        assertTrue(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithShortCVC() {
        Card card = new Card("378282246310005", 12, 2050, "123");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithLongCVC() {
        Card card = new Card("378282246310005", 12, 2050, "12345");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithBadCVC() {
        Card card = new Card("378282246310005", 12, 2050, "bad");
        assertFalse(card.validateCard());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    public void testLast4() {
        Card card = new Card("42 42 42 42 42 42 42 42", null, null, null);
        assertEquals("4242", card.getLast4());
    }

    @Test
    public void last4ShouldBeNullWhenNumberIsNull() {
        Card card = new Card(null, null, null, null);
        assertEquals(null, card.getLast4());
    }
}
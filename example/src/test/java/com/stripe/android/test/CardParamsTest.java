package com.stripe.android.test;

import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.stripe.android.model.CardParams;
import com.stripe.android.time.FrozenClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CardParamsTest {
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
        CardParams card = new CardParams("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateNumber());
    }

    @Test
    public void testTypeReturnsCorrectlyForAmexCard() {
        CardParams card = new CardParams("3412123412341234", null, null, null);
        assertEquals("American Express", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForDiscoverCard() {
        CardParams card = new CardParams("6452123412341234", null, null, null);
        assertEquals("Discover", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForJCBCard() {
        CardParams card = new CardParams("3512123412341234", null, null, null);
        assertEquals("JCB", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForDinersClubCard() {
        CardParams card = new CardParams("3612123412341234", null, null, null);
        assertEquals("Diners Club", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForVisaCard() {
        CardParams card = new CardParams("4112123412341234", null, null, null);
        assertEquals("Visa", card.getType());
    }

    @Test
    public void testTypeReturnsCorrectlyForMasterCard() {
        CardParams card = new CardParams("5112123412341234", null, null, null);
        assertEquals("MasterCard", card.getType());
    }

    @Test
    public void shouldPassValidateNumberIfLuhnNumber() {
        CardParams card = new CardParams("4242-4242-4242-4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfNotLuhnNumber() {
        CardParams card = new CardParams("4242-4242-4242-4241", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberIfLuhnNumberAmex() {
        CardParams card = new CardParams("378282246310005", null, null, null);
        assertEquals("American Express", card.getType());
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfNull() {
        CardParams card = new CardParams(null, null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfBlank() {
        CardParams card = new CardParams("", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfJustSpaces() {
        CardParams card = new CardParams("    ", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfTooShort() {
        CardParams card = new CardParams("0", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfContainsLetters() {
        CardParams card = new CardParams("424242424242a4242", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfTooLong() {
        CardParams card = new CardParams("4242 4242 4242 4242 6", null, null, null);
        assertEquals("Visa", card.getType());
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumber() {
        CardParams card = new CardParams("4242424242424242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberSpaces() {
        CardParams card = new CardParams("4242 4242 4242 4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberDashes() {
        CardParams card = new CardParams("4242-4242-4242-4242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldPassValidateNumberWithMixedSeparators() {
        CardParams card = new CardParams("4242-4   242 424-24 242", null, null, null);
        assertTrue(card.validateNumber());
    }

    @Test
    public void shouldFailValidateNumberIfWithDot() {
        CardParams card = new CardParams("4242.4242.4242.4242", null, null, null);
        assertFalse(card.validateNumber());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNull() {
        CardParams card = new CardParams(null, null, null, null);
        assertFalse(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNullMonth() {
        CardParams card = new CardParams(null, null, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfZeroMonth() {
        CardParams card = new CardParams(null, 0, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNegativeMonth() {
        CardParams card = new CardParams(null, -1, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfMonthToLarge() {
        CardParams card = new CardParams(null, 13, YEAR_IN_FUTURE, null);
        assertFalse(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNullYear() {
        CardParams card = new CardParams(null, 1, null, null);
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfZeroYear() {
        CardParams card = new CardParams(null, 12, 0, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfNegativeYear() {
        CardParams card = new CardParams(null, 12, -1, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateForDecemberOfThisYear() {
        CardParams card = new CardParams(null, 12, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonth() {
        CardParams card = new CardParams(null, 8, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonthTwoDigitYear() {
        CardParams card = new CardParams(null, 8, 97, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfLastMonth() {
        CardParams card = new CardParams(null, 7, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfNextMonth() {
        CardParams card = new CardParams(null, 9, 1997, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateForJanuary00() {
        CardParams card = new CardParams(null, 1, 0, null);
        assertTrue(card.validateExpMonth());
        assertFalse(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateForDecember99() {
        CardParams card = new CardParams(null, 12, 99, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateCVCIfNull() {
        CardParams card = new CardParams(null, null, null, null);
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfBlank() {
        CardParams card = new CardParams(null, null, null, "");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfUnknownTypeAndLength2() {
        CardParams card = new CardParams(null, null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfUnknownTypeAndLength3() {
        CardParams card = new CardParams(null, null, null, "123");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfUnknownTypeAndLength4() {
        CardParams card = new CardParams(null, null, null, "1234");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfUnknownTypeAndLength5() {
        CardParams card = new CardParams(null, null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength2() {
        CardParams card = new CardParams("4242 4242 4242 4242", null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfVisaAndLength3() {
        CardParams card = new CardParams("4242 4242 4242 4242", null, null, "123");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength4() {
        CardParams card = new CardParams("4242 4242 4242 4242", null, null, "1234");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndLength5() {
        CardParams card = new CardParams("4242 4242 4242 4242", null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfVisaAndNotNumeric() {
        CardParams card = new CardParams("4242 4242 4242 4242", null, null, "12a");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfAmexAndLength2() {
        CardParams card = new CardParams("378282246310005", null, null, "12");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndLength3() {
        CardParams card = new CardParams("378282246310005", null, null, "123");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCVCIfAmexAndLength4() {
        CardParams card = new CardParams("378282246310005", null, null, "1234");
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndLength5() {
        CardParams card = new CardParams("378282246310005", null, null, "12345");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCVCIfAmexAndNotNumeric() {
        CardParams card = new CardParams("378282246310005", null, null, "123d");
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardIfNotLuhnNumber() {
        CardParams card = new CardParams("4242-4242-4242-4241", 12, 2050, "123");
        assertFalse(card.validateCardParams());
        assertFalse(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardInvalidMonth() {
        CardParams card = new CardParams("4242-4242-4242-4242", 13, 2050, "123");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertFalse(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardInvalidYear() {
        CardParams card = new CardParams("4242-4242-4242-4242", 1, 1990, "123");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertFalse(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardWithNullCVC() {
        CardParams card = new CardParams("4242-4242-4242-4242", 12, 2050, null);
        assertTrue(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardVisa() {
        CardParams card = new CardParams("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithShortCVC() {
        CardParams card = new CardParams("4242-4242-4242-4242", 12, 2050, "12");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithLongCVC() {
        CardParams card = new CardParams("4242-4242-4242-4242", 12, 2050, "1234");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardVisaWithBadCVC() {
        CardParams card = new CardParams("4242-4242-4242-4242", 12, 2050, "bad");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardAmex() {
        CardParams card = new CardParams("378282246310005", 12, 2050, "1234");
        assertTrue(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertTrue(card.validateCVC());
    }

    @Test
    public void shouldPassValidateCardAmexWithNullCVC() {
        CardParams card = new CardParams("378282246310005", 12, 2050, null);
        assertTrue(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithShortCVC() {
        CardParams card = new CardParams("378282246310005", 12, 2050, "123");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithLongCVC() {
        CardParams card = new CardParams("378282246310005", 12, 2050, "12345");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    @Test
    public void shouldFailValidateCardAmexWithBadCVC() {
        CardParams card = new CardParams("378282246310005", 12, 2050, "bad");
        assertFalse(card.validateCardParams());
        assertTrue(card.validateNumber());
        assertTrue(card.validateExpiryDate());
        assertFalse(card.validateCVC());
    }

    public void testLast4() {
        CardParams card = new CardParams("42 42 42 42 42 42 42 42", null, null, null);
        assertEquals("4242", card.getLast4());
    }

    @Test
    public void last4ShouldBeNullWhenNumberIsNull() {
        CardParams card = new CardParams(null, null, null, null);
        assertEquals(null, card.getLast4());
    }
}
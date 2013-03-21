package com.stripe.android.test;

import java.util.Calendar;

import org.junit.Test;

import com.stripe.android.model.Card;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CardTest {
	private static final int YEAR_IN_FUTURE = 9999;

    @Test
    public void canInitializeWithMinimalArguments() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateNumber());
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
    public void shouldPassValidateNumberWithoutDashes() {
        Card card = new Card("4242424242424242", null, null, null);
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
    	Calendar cal = Calendar.getInstance();
    	int year = cal.get(Calendar.YEAR);

        Card card = new Card(null, 12, year, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonth() {
    	Calendar cal = Calendar.getInstance();
    	int month = cal.get(Calendar.MONTH) + 1;
    	int year = cal.get(Calendar.YEAR);

        Card card = new Card(null, month, year, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfCurrentMonthTwoDigitYear() {
    	Calendar cal = Calendar.getInstance();
    	int month = cal.get(Calendar.MONTH) + 1;
    	String year = String.valueOf(cal.get(Calendar.YEAR));
    	String suffix = year.substring(year.length() - 2, year.length());
    	int twoDigitYear = Integer.parseInt(suffix);

        Card card = new Card(null, month, twoDigitYear, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertTrue(card.validateExpiryDate());
    }

    @Test
    public void shouldFailValidateExpiryDateIfLastMonth() {
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.MONTH, -1);
    	int month = cal.get(Calendar.MONTH) + 1;
    	int year = cal.get(Calendar.YEAR);

        Card card = new Card(null, month, year, null);
        assertTrue(card.validateExpMonth());
        assertTrue(card.validateExpYear());
        assertFalse(card.validateExpiryDate());
    }

    @Test
    public void shouldPassValidateExpiryDateIfNextMonth() {
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.MONTH, 1);
    	int month = cal.get(Calendar.MONTH) + 1;
    	int year = cal.get(Calendar.YEAR);

        Card card = new Card(null, month, year, null);
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
    public void shouldPassValidateCVCIfAmexAndLength5() {
        Card card = new Card("378282246310005", null, null, "12345");
        assertFalse(card.validateCVC());
    }
}
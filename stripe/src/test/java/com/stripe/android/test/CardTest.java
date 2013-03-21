package com.stripe.android.test;

import org.junit.Test;

import com.stripe.android.model.Card;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CardTest {

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
}
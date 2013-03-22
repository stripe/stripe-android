package com.stripe.android.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.stripe.android.util.CardNumberFormatter;

public class CardNumberFormatterTest {
    @Test
    public void testNull() {
        String formatted = CardNumberFormatter.format(null, false);
        assertNull(formatted);
    }

    @Test
    public void testNullTrailing() {
        String formatted = CardNumberFormatter.format(null, true);
        assertNull(formatted);
    }

    @Test
    public void testEmpty() {
        String formatted = CardNumberFormatter.format("", false);
        assertEquals("", formatted);
    }

    @Test
    public void testEmptyTrailing() {
        String formatted = CardNumberFormatter.format("", true);
        assertEquals("", formatted);
    }

    @Test
    public void test1Digit() {
        String formatted = CardNumberFormatter.format("1", false);
        assertEquals("1", formatted);
    }

    @Test
    public void test1DigitTrailing() {
        String formatted = CardNumberFormatter.format("1", true);
        assertEquals("1", formatted);
    }

    @Test
    public void testUnknown4DigitsTrailing() {
        String formatted = CardNumberFormatter.format("0000", true);
        assertEquals("0000", formatted);
    }

    @Test
    public void testVisa3Digits() {
        String formatted = CardNumberFormatter.format("424", false);
        assertEquals("424", formatted);
    }

    @Test
    public void testVisa3DigitsTrailing() {
        String formatted = CardNumberFormatter.format("424", true);
        assertEquals("424", formatted);
    }

    @Test
    public void testVisa4Digits() {
        String formatted = CardNumberFormatter.format("4242", false);
        assertEquals("4242", formatted);
    }

    @Test
    public void testVisa4DigitsTrailing() {
        String formatted = CardNumberFormatter.format("4242", true);
        assertEquals("4242 ", formatted);
    }

    @Test
    public void testVisa5Digits() {
        String formatted = CardNumberFormatter.format("42424", false);
        assertEquals("4242 4", formatted);
    }

    @Test
    public void testVisa5DigitsTrailing() {
        String formatted = CardNumberFormatter.format("42424", true);
        assertEquals("4242 4", formatted);
    }

    @Test
    public void testVisa12Digits() {
        String formatted = CardNumberFormatter.format("424242424242", false);
        assertEquals("4242 4242 4242", formatted);
    }

    @Test
    public void testVisa12DigitsTrailing() {
        String formatted = CardNumberFormatter.format("424242424242", true);
        assertEquals("4242 4242 4242 ", formatted);
    }

    @Test
    public void testFullVisa() {
        String formatted = CardNumberFormatter.format("4242424242424242", false);
        assertEquals("4242 4242 4242 4242", formatted);
    }

    @Test
    public void testFullVisaTrailing() {
        String formatted = CardNumberFormatter.format("4242424242424242", true);
        assertEquals("4242 4242 4242 4242", formatted);
    }

    @Test
    public void testFullVisaWithSpaces() {
        String formatted = CardNumberFormatter.format("4 2424242   42424242", false);
        assertEquals("4242 4242 4242 4242", formatted);
    }

    @Test
    public void testFullVisaWithOtherCharacters() {
        String formatted = CardNumberFormatter.format("4242,424a242 xyz 424242", false);
        assertEquals("4242 4242 4242 4242", formatted);
    }

    @Test
    public void testAmex4Digits() {
        String formatted = CardNumberFormatter.format("3728", false);
        assertEquals("3728", formatted);
    }

    @Test
    public void testAmex4DigitsTrailing() {
        String formatted = CardNumberFormatter.format("3728", true);
        assertEquals("3728 ", formatted);
    }

    @Test
    public void testAmex5Digits() {
        String formatted = CardNumberFormatter.format("37288", false);
        assertEquals("3728 8", formatted);
    }

    @Test
    public void testAmex5DigitsTrailing() {
        String formatted = CardNumberFormatter.format("37288", true);
        assertEquals("3728 8", formatted);
    }

    @Test
    public void testAmex10Digits() {
        String formatted = CardNumberFormatter.format("3782822463", false);
        assertEquals("3782 822463", formatted);
    }

    @Test
    public void testAmex10DigitsTrailing() {
        String formatted = CardNumberFormatter.format("3782822463", true);
        assertEquals("3782 822463 ", formatted);
    }

    @Test
    public void testFullAmex() {
        String formatted = CardNumberFormatter.format("378282246310005", false);
        assertEquals("3782 822463 10005", formatted);
    }

    @Test
    public void testFullAmexTrailing() {
        String formatted = CardNumberFormatter.format("378282246310005", true);
        assertEquals("3782 822463 10005", formatted);
    }

    @Test
    public void testFullAmexWithSpaces() {
        String formatted = CardNumberFormatter.format("378 282 246 310 005", false);
        assertEquals("3782 822463 10005", formatted);
    }

    @Test
    public void testFullAmexWIthOtherCharacters() {
        String formatted = CardNumberFormatter.format("3782c8224_631  something 0005", false);
        assertEquals("3782 822463 10005", formatted);
    }

}

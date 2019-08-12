package com.stripe.android.view;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShippingPostalCodeValidatorTest {
    private static final ShippingPostalCodeValidator VALIDATOR = new ShippingPostalCodeValidator();

    @Test
    public void testPostalCodeOptional() {
        assertTrue(VALIDATOR.isValid("", "US",
                Collections.singletonList(
                        ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD),
                Collections.<String>emptyList()));
    }

    @Test
    public void usZipCodeTest() {
        assertTrue(isValid("94107", "US"));
        assertTrue(isValid("94107-1234", "US"));
        assertFalse(isValid("941071234", "US"));
        assertFalse(isValid("9410a1234", "US"));
        assertFalse(isValid("94107-", "US"));
        assertFalse(isValid("9410&", "US"));
        assertFalse(isValid("K1A 0B1", "US"));
        assertFalse(isValid("", "US"));
    }

    @Test
    public void canadianPostalCodeTest() {
        assertTrue(isValid("K1A 0B1", "CA"));
        assertTrue(isValid("B1Z 0B9", "CA"));
        assertFalse(isValid("K1A 0D1", "CA"));
        assertFalse(isValid("94107", "CA"));
        assertFalse(isValid("94107-1234", "CA"));
        assertFalse(isValid("W1A 0B1", "CA"));
        assertFalse(isValid("123", "CA"));
        assertFalse(isValid("", "CA"));
    }

    @Test
    public void ukPostalCodeTest() {
        assertTrue(isValid("L1 8JQ", "GB"));
        assertTrue(isValid("GU16 7HF", "GB"));
        assertTrue(isValid("PO16 7GZ", "GB"));
        assertFalse(isValid("94107", "GB"));
        assertFalse(isValid("94107-1234", "GB"));
        assertFalse(isValid("!1A 0B1", "GB"));
        assertFalse(isValid("Z1A 0B1", "GB"));
        assertFalse(isValid("123", "GB"));
    }

    private boolean isValid(@NonNull String input, @NonNull String countryCode) {
        return VALIDATOR.isValid(input, countryCode,
                Collections.<String>emptyList(), Collections.<String>emptyList());
    }
}

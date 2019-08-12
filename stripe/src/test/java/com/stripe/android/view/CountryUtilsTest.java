package com.stripe.android.view;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CountryUtils}
 */
public class CountryUtilsTest {

    @Test
    public void postalCodeCountryTest() {
        assertTrue(CountryUtils.doesCountryUsePostalCode("US"));
        assertTrue(CountryUtils.doesCountryUsePostalCode("UK"));
        assertTrue(CountryUtils.doesCountryUsePostalCode("CA"));
        assertFalse(CountryUtils.doesCountryUsePostalCode("DM"));
    }
}

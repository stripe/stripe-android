package com.stripe.android.view;

import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void getOrderedCountries() {
        assertEquals(
                Locale.getDefault().getDisplayCountry(),
                CountryUtils.getOrderedCountries(Locale.getDefault()).get(0)
        );
    }
}

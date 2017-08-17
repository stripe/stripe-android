package com.stripe.android.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CountryUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CountryUtilsTest {

    @Test
    public void postalCodeCountryTest() {
        assertTrue(CountryUtils.doesCountryUsePostalCode("US"));
        assertTrue(CountryUtils.doesCountryUsePostalCode("UK"));
        assertTrue(CountryUtils.doesCountryUsePostalCode("CA"));
        assertFalse(CountryUtils.doesCountryUsePostalCode("DM"));
    }

}

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

    @Test
    public void usZipCodeTest() {
        assertTrue(CountryUtils.isUSZipCodeValid("94107"));
        assertTrue(CountryUtils.isUSZipCodeValid("94107-1234"));
        assertFalse(CountryUtils.isUSZipCodeValid("941071234"));
        assertFalse(CountryUtils.isUSZipCodeValid("9410a1234"));
        assertFalse(CountryUtils.isUSZipCodeValid("94107-"));
        assertFalse(CountryUtils.isUSZipCodeValid("9410&"));
        assertFalse(CountryUtils.isUSZipCodeValid("K1A 0B1"));
        assertFalse(CountryUtils.isUSZipCodeValid(""));
    }

    @Test
    public void canadianPostalCodeTest() {
        assertTrue(CountryUtils.isCanadianPostalCodeValid("K1A 0B1"));
        assertTrue(CountryUtils.isCanadianPostalCodeValid("B1Z 0B9"));
        assertFalse(CountryUtils.isCanadianPostalCodeValid("K1A 0D1"));
        assertFalse(CountryUtils.isCanadianPostalCodeValid("94107"));
        assertFalse(CountryUtils.isCanadianPostalCodeValid("94107-1234"));
        assertFalse(CountryUtils.isCanadianPostalCodeValid("W1A 0B1"));
        assertFalse(CountryUtils.isCanadianPostalCodeValid("123"));
        assertFalse(CountryUtils.isCanadianPostalCodeValid(""));
    }

    @Test
    public void ukPostalCodeTest() {
        assertTrue(CountryUtils.isUKPostcodeValid("L1 8JQ"));
        assertTrue(CountryUtils.isUKPostcodeValid("GU16 7HF"));
        assertTrue(CountryUtils.isUKPostcodeValid("PO16 7GZ"));
        assertFalse(CountryUtils.isUKPostcodeValid("94107"));
        assertFalse(CountryUtils.isUKPostcodeValid("94107-1234"));
        assertFalse(CountryUtils.isUKPostcodeValid("!1A 0B1"));
        assertFalse(CountryUtils.isUKPostcodeValid("Z1A 0B1"));
        assertFalse(CountryUtils.isUKPostcodeValid("123"));
    }
}

package com.stripe.samplestore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Currency;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link StoreUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StoreUtilsTest {
    
    @Test
    public void getPriceString_withUSD_returnsExpectedValues() {
        Currency dollars = Currency.getInstance("USD");
        String firstPrice = StoreUtils.getPriceString(1000, dollars);
        String secondPrice = StoreUtils.getPriceString(55, dollars);
        String tinyPrice = StoreUtils.getPriceString(7, dollars);
        String bigPrice = StoreUtils.getPriceString(1234567890, dollars);
        assertEquals("$10.00", firstPrice);
        assertEquals("$0.55", secondPrice);
        assertEquals("$0.07", tinyPrice);
        assertEquals("$12345678.90", bigPrice);
    }
    
    @Test
    public void getPriceString_withYen_returnsExpectedValues() {
        Currency yen = Currency.getInstance("JPY");
        String firstPrice = StoreUtils.getPriceString(1000, yen);
        String secondPrice = StoreUtils.getPriceString(55, yen);
        String bigPrice = StoreUtils.getPriceString(1234567890, yen);
        assertEquals("JPY1000", firstPrice);
        assertEquals("JPY55", secondPrice);
        assertEquals("JPY1234567890", bigPrice);
    }
}

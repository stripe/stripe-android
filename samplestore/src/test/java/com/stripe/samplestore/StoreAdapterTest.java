package com.stripe.samplestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Currency;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link StoreAdapter}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StoreAdapterTest {

    @Test
    public void getPriceString_withUSD_returnsExpectedValues() {
        Currency dollars = Currency.getInstance("USD");
        String firstPrice = StoreAdapter.getPriceString(1000, dollars);
        String secondPrice = StoreAdapter.getPriceString(55, dollars);
        String tinyPrice = StoreAdapter.getPriceString(7, dollars);
        String bigPrice = StoreAdapter.getPriceString(1234567890, dollars);
        assertEquals("$10.00", firstPrice);
        assertEquals("$0.55", secondPrice);
        assertEquals("$0.07", tinyPrice);
        assertEquals("$12345678.90", bigPrice);
    }

    @Test
    public void getPriceString_withYen_returnsExpectedValues() {
        Currency yen = Currency.getInstance("JPY");
        String firstPrice = StoreAdapter.getPriceString(1000, yen);
        String secondPrice = StoreAdapter.getPriceString(55, yen);
        String bigPrice = StoreAdapter.getPriceString(1234567890, yen);
        assertEquals("JPY1000", firstPrice);
        assertEquals("JPY55", secondPrice);
        assertEquals("JPY1234567890", bigPrice);
    }
}

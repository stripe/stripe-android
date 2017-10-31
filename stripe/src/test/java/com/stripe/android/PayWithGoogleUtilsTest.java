package com.stripe.android;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Currency;
import java.util.Locale;

import static com.stripe.android.PayWithGoogleUtils.getPriceString;
import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link PayWithGoogleUtils}.
 */
@RunWith(RobolectricTestRunner.class)
public class PayWithGoogleUtilsTest {

    @After
    public void tearDown() {
        Locale.setDefault(Locale.US);
    }

    @Test
    public void getPriceString_whenCurrencyWithDecimals_returnsExpectedValue() {
        String priceString = getPriceString(100L, Currency.getInstance("USD"));
        assertEquals("1.00", priceString);

        String littlePrice = getPriceString(8L, Currency.getInstance("EUR"));
        assertEquals("0.08", littlePrice);

        String bigPrice = getPriceString(20000000L, Currency.getInstance("GBP"));
        assertEquals("200000.00", bigPrice);
    }

    @Test
    public void getPriceString_whenLocaleWithCommas_returnsExpectedValue() {
        Locale.setDefault(Locale.FRENCH);
        String priceString = getPriceString(100L, Currency.getInstance("USD"));
        assertEquals("1.00", priceString);

        String littlePrice = getPriceString(8L, Currency.getInstance("EUR"));
        assertEquals("0.08", littlePrice);

        String bigPrice = getPriceString(20000000L, Currency.getInstance("GBP"));
        assertEquals("200000.00", bigPrice);
    }

    @Test
    public void getPriceString_whenCurrencyWithoutDecimals_returnsExpectedValue() {
        String priceString = getPriceString(250L, Currency.getInstance("JPY"));
        assertEquals("250", priceString);

        String bigPrice = getPriceString(250000L, Currency.getInstance("KRW"));
        assertEquals("250000", bigPrice);

        String littlePrice = getPriceString(7L, Currency.getInstance("CLP"));
        assertEquals("7", littlePrice);
    }
}

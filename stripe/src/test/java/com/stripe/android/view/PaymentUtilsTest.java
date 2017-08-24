package com.stripe.android.view;

import com.stripe.android.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Currency;
import java.util.Locale;

import static junit.framework.Assert.assertEquals;

/**
 * Test class for {@link PaymentUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25, constants = BuildConfig.class)
public class PaymentUtilsTest {


    @Before
    public void setup() {
        Locale.setDefault(Locale.US);
    }

    @Test
    public void formatPriceStringUsingFree_whenZero_rendersFree() {
        assertEquals(
                PaymentUtils.formatPriceStringUsingFree(
                        0,
                        Currency.getInstance("USD"),
                        "Free"),
                        "Free");
    }

    @Test
    public void formatPriceString_whenUSLocale_rendersCorrectSymbols() {

        Locale.setDefault(Locale.US);
        assertEquals(PaymentUtils.formatPriceString(12300, Currency.getInstance("USD")), "$123.00");

        Currency euro = Currency.getInstance(Locale.GERMANY);
        assertEquals(PaymentUtils.formatPriceString(12300, euro), "EUR123.00");

        Currency canadianDollar = Currency.getInstance(Locale.CANADA);
        assertEquals(PaymentUtils.formatPriceString(12300, canadianDollar), "CAD123.00");

        Currency britishPound = Currency.getInstance(Locale.UK);
        assertEquals(PaymentUtils.formatPriceString(10000, britishPound), "GBP100.00");
    }

    @Test
    public void formatPriceString_whenInternationalLocale_rendersCorrectSymbols() {

        Locale.setDefault(Locale.GERMANY);
        Currency euro = Currency.getInstance(Locale.GERMANY);
        assertEquals(PaymentUtils.formatPriceString(10000, euro), "100,00 €");

        Locale.setDefault(Locale.JAPAN);
        Currency yen = Currency.getInstance(Locale.JAPAN);
        // Japan's native local uses narrow yen symbol (there is also a wide yen symbol)
        assertEquals(PaymentUtils.formatPriceString(100, yen), "￥100");

        Locale.setDefault(new Locale("ar", "JO")); //Jordan
        Currency jordanianDinar = Currency.getInstance("JOD");
        assertEquals(PaymentUtils.formatPriceString(100123, jordanianDinar), jordanianDinar.getSymbol() + " 100.123");

        Locale.setDefault(Locale.UK);
        Currency britishPound = Currency.getInstance(Locale.UK);
        assertEquals(PaymentUtils.formatPriceString(10000, britishPound), "£100.00");

        Locale.setDefault(Locale.CANADA);
        Currency canadianDollar = Currency.getInstance(Locale.CANADA);
        assertEquals(PaymentUtils.formatPriceString(12300, canadianDollar), "$123.00");
    }

    @Test
    public void formatPriceString_whenDecimalAmounts_rendersCorrectDigits() {
        assertEquals(PaymentUtils.formatPriceString(10012, Currency.getInstance("USD")), "$100.12");
        assertEquals(PaymentUtils.formatPriceString(12, Currency.getInstance("USD")), "$0.12");
    }

}

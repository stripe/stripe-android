package com.stripe.android.view;

import android.support.v7.app.AppCompatActivity;

import com.stripe.android.BuildConfig;
import com.stripe.android.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
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

    ActivityController<AppCompatActivity> mActivityController;

    @Before
    public void setup() {
        Locale.setDefault(Locale.US);
        mActivityController = Robolectric.buildActivity(AppCompatActivity.class).create().start();
    }

    @Test
    public void formatPriceStringUsingFree_whenZero_rendersFree() {
        assertEquals(
                PaymentUtils.formatPriceStringUsingFree(
                        mActivityController.get(), 0,
                        Currency.getInstance("USD")),
                        mActivityController.get().getResources().getString(R.string.price_free));
    }

    @Test
    public void formatPriceString_whenUSLocale_rendersCorrectSymbols() {

        Locale.setDefault(Locale.US);
        assertEquals(PaymentUtils.formatPriceString(123, Currency.getInstance("USD")), "$123.00");

        Currency euro = Currency.getInstance(Locale.GERMANY);
        assertEquals(PaymentUtils.formatPriceString(123, euro), "EUR123.00");

        Currency canadianDollar = Currency.getInstance(Locale.CANADA);
        assertEquals(PaymentUtils.formatPriceString(123, canadianDollar), "CAD123.00");

        Currency britishPound = Currency.getInstance(Locale.UK);
        assertEquals(PaymentUtils.formatPriceString(100, britishPound), "GBP100.00");
    }

    @Test
    public void formatPriceString_whenInternationalLocale_rendersCorrectSymbols() {

        Locale.setDefault(Locale.GERMANY);
        Currency euro = Currency.getInstance(Locale.GERMANY);
        assertEquals(PaymentUtils.formatPriceString(100, euro), "100,00 €");

        Locale.setDefault(Locale.JAPAN);
        Currency yen = Currency.getInstance(Locale.JAPAN);
        // Japan's native local uses narrow yen symbol (there is also a wide yen symbol)
        assertEquals(PaymentUtils.formatPriceString(100, yen), "￥100");

        Locale.setDefault(new Locale("ar", "JO")); //Jordan
        Currency jordanianDinar = Currency.getInstance("JOD");
        assertEquals(PaymentUtils.formatPriceString(100.123, jordanianDinar), jordanianDinar.getSymbol() + " 100.123");

        Locale.setDefault(Locale.UK);
        Currency britishPound = Currency.getInstance(Locale.UK);
        assertEquals(PaymentUtils.formatPriceString(100, britishPound), "£100.00");

        Locale.setDefault(Locale.CANADA);
        Currency canadianDollar = Currency.getInstance(Locale.CANADA);
        assertEquals(PaymentUtils.formatPriceString(123, canadianDollar), "$123.00");
    }

    @Test
    public void formatPriceString_whenDecimalAmounts_rendersCorrectDigits() {
        assertEquals(PaymentUtils.formatPriceString(100.123, Currency.getInstance("USD")), "$100.12");
        assertEquals(PaymentUtils.formatPriceString(.12, Currency.getInstance("USD")), "$0.12");
    }

}

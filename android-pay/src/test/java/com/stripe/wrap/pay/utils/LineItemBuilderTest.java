package com.stripe.wrap.pay.utils;

import android.util.Log;

import com.google.android.gms.wallet.LineItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static com.stripe.wrap.pay.utils.LineItemBuilder.isPriceBreakdownConsistent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link LineItemBuilder}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class LineItemBuilderTest {

    @Test
    public void emptyLineItemBuilder_createsEmptyLineItemWithDefaults() {
        Locale.setDefault(Locale.US);

        LineItemBuilder lineItemBuilder = new LineItemBuilder();
        LineItem item = lineItemBuilder.build();
        assertEquals(LineItem.Role.REGULAR, item.getRole());
        assertEquals(Currency.getInstance(Locale.US).getCurrencyCode(), item.getCurrencyCode());
    }

    @Test
    public void setAllAttributes_thenBuild_createsExpectedLineItem() {
        final String currencyCode = "EUR";
        final String description = "a test item";

        LineItemBuilder lineItemBuilder = new LineItemBuilder(currencyCode);
        LineItem lineItem = lineItemBuilder.setDescription(description)
                .setUnitPrice(100)
                .setQuantity(2)
                .setTotalPrice(200)
                .build();
        assertEquals(LineItem.Role.REGULAR, lineItem.getRole());
        assertEquals(currencyCode, lineItem.getCurrencyCode());
        assertEquals(description, lineItem.getDescription());
        assertEquals("2", lineItem.getQuantity());
        assertEquals("1.00", lineItem.getUnitPrice());
        assertEquals("2.00", lineItem.getTotalPrice());
    }

    @Test
    public void setHighPrice_thenBuild_createsExpectedLineItem() {
        LineItemBuilder lineItemBuilder = new LineItemBuilder("USD");
        LineItem lineItem = lineItemBuilder
                .setUnitPrice(1000000L)
                .setQuantity(2)
                .setTotalPrice(2000000L)
                .setDescription("Best smart watch ever")
                .build();
        assertEquals(LineItem.Role.REGULAR, lineItem.getRole());
        assertEquals("USD", lineItem.getCurrencyCode());
        assertEquals("Best smart watch ever", lineItem.getDescription());
        assertEquals("2", lineItem.getQuantity());
        assertEquals("10000.00", lineItem.getUnitPrice());
        assertEquals("20000.00", lineItem.getTotalPrice());
    }

    @Test
    public void setCurrency_withLowerCaseString_stillSetsCurrency() {
        // If you try to create a Currency object with a lower-case code, it throws
        // an IllegalArgumentException.
        LineItemBuilder builder = new LineItemBuilder("eur");
        LineItem item = builder.build();
        assertEquals("EUR", item.getCurrencyCode());
    }

    @Test
    public void setQuantityAndUnitPrice_whenNoTotalPriceSet_createsTotalPrice() {
        LineItemBuilder builder = new LineItemBuilder("usd");
        LineItem item = builder.setQuantity(1.5)
                .setUnitPrice(399)
                .build();

        assertEquals("1.5", item.getQuantity());
        assertEquals("3.99", item.getUnitPrice());
        assertEquals("5.98", item.getTotalPrice());
    }

    @Test
    public void setQuantity_whenMoreThanOneDigitAfterDecimal_getsRoundedAndLogsWarning() {
        ShadowLog.stream = System.out;
        Locale.setDefault(Locale.US);
        LineItem item = new LineItemBuilder().setQuantity(1.71).build();

        String expectedWarning = String.format(
                Locale.ENGLISH,
                "Tried to create quantity %.2f, but Android Pay quantity" +
                        " may only have one digit after decimal. Value was rounded to 1.7",
                1.71);
        List<ShadowLog.LogItem> logItems = ShadowLog.getLogsForTag(LineItemBuilder.TAG);
        assertFalse(logItems.isEmpty());
        assertEquals(1, logItems.size());
        assertEquals(expectedWarning, logItems.get(0).msg);
        assertEquals(Log.WARN, logItems.get(0).type);
        assertEquals("1.7", item.getQuantity());
    }

    @Test
    public void setCurrencyCode_whenInvalid_setsToLocaleDefaultAndLogsWarning() {
        ShadowLog.stream = System.out;
        Locale.setDefault(Locale.JAPAN);

        LineItem item = new LineItemBuilder("notacurrency").build();
        String expectedWarning = "Could not create currency with code \"notacurrency\". Using " +
                "currency JPY by default.";
        List<ShadowLog.LogItem> logItems = ShadowLog.getLogsForTag(PaymentUtils.TAG);
        assertEquals("JPY", item.getCurrencyCode());
        assertEquals(1, logItems.size());
        assertEquals(Log.WARN, logItems.get(0).type);
        assertEquals(expectedWarning, logItems.get(0).msg);
    }

    @Test
    public void isWholeNumber_whenBigDecimalFromInteger_returnsTrue() {
        assertTrue(LineItemBuilder.isWholeNumber(BigDecimal.ZERO));
        assertTrue(LineItemBuilder.isWholeNumber(BigDecimal.valueOf(555)));
    }

    @Test
    public void isWholeNumber_whenBigDecimalDoubleWithoutDecimalPart_returnsTrue() {
        assertTrue(LineItemBuilder.isWholeNumber(BigDecimal.valueOf(1.0000)));
    }

    @Test
    public void isWholeNumber_whenBigDecimalDoubleWithDecimalPart_returnsFalse() {
        assertFalse(LineItemBuilder.isWholeNumber(BigDecimal.valueOf(1.5)));
        assertFalse(LineItemBuilder.isWholeNumber(BigDecimal.valueOf(2.07)));
    }

    @Test
    public void isPriceBreakdownConsistent_whenItemsMultiplyClosely_returnsTrue() {
        assertTrue(isPriceBreakdownConsistent(1000L, BigDecimal.TEN, 10000L));
    }

    @Test
    public void isPriceBreakdownConsistent_whenAnyItemIsNull_returnsTrue() {
        assertTrue(isPriceBreakdownConsistent(null, BigDecimal.TEN, 1234L));
        assertTrue(isPriceBreakdownConsistent(55L, null, 8888L));
        assertTrue(isPriceBreakdownConsistent(33L, BigDecimal.ONE, null));
    }

    @Test
    public void isPriceBreakdownConsistent_whenItemsAreOffByALot_returnsFalse() {
        assertFalse(isPriceBreakdownConsistent(55L, BigDecimal.ONE, 56L));
    }

    @Test
    public void isPriceBreakdownConsistent_whenItemsAreOffByOnlyALittle_returnsTrue() {
        assertTrue(isPriceBreakdownConsistent(199L, BigDecimal.ONE, 200L));
    }

    @Test
    public void build_whenPriceBreakdownIsNotConsistent_logsWarning() {
        ShadowLog.stream = System.out;
        Locale.setDefault(Locale.JAPAN);

        LineItem item = new LineItemBuilder("USD")
                .setQuantity(1.0)
                .setUnitPrice(1500L)
                .setTotalPrice(2000L).build();


        String expectedWarning = "Price breakdown of 1500 * 1.0 = 2000 is off by more than 1 percent";
        List<ShadowLog.LogItem> logItems = ShadowLog.getLogsForTag(LineItemBuilder.TAG);
        assertEquals("15.00", item.getUnitPrice());
        assertEquals("20.00", item.getTotalPrice());
        assertEquals("1", item.getQuantity());
        assertEquals(1, logItems.size());
        assertEquals(Log.WARN, logItems.get(0).type);
        assertEquals(expectedWarning, logItems.get(0).msg);
    }
}

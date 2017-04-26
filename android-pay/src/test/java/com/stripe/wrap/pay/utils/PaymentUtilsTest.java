package com.stripe.wrap.pay.utils;

import com.google.android.gms.wallet.LineItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static com.stripe.wrap.pay.utils.PaymentUtils.getCurrencyByCodeOrDefault;
import static com.stripe.wrap.pay.utils.PaymentUtils.getPriceLong;
import static com.stripe.wrap.pay.utils.PaymentUtils.getPriceString;
import static com.stripe.wrap.pay.utils.PaymentUtils.getTotalPriceString;
import static com.stripe.wrap.pay.utils.PaymentUtils.isLineItemListValid;
import static com.stripe.wrap.pay.utils.PaymentUtils.isLineItemValid;
import static com.stripe.wrap.pay.utils.PaymentUtils.matchesCurrencyPatternOrEmpty;
import static com.stripe.wrap.pay.utils.PaymentUtils.matchesQuantityPatternOrEmpty;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertTrue;

/**
 * Test class for {@link PaymentUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class PaymentUtilsTest {

    @Test
    public void matchesCurrencyPattern_whenValid_returnsTrue() {
        assertTrue(matchesCurrencyPatternOrEmpty(null));
        assertTrue(matchesCurrencyPatternOrEmpty(""));
        assertTrue(matchesCurrencyPatternOrEmpty("10.00"));
        assertTrue(matchesCurrencyPatternOrEmpty("01.11"));
        assertTrue(matchesCurrencyPatternOrEmpty("5000.05"));
        assertTrue(matchesCurrencyPatternOrEmpty("100"));
        assertTrue(matchesCurrencyPatternOrEmpty("-5"));
    }

    @Test
    public void matchesCurrencyPattern_whenInvalid_returnsFalse() {
        assertFalse(matchesCurrencyPatternOrEmpty(".99"));
        assertFalse(matchesCurrencyPatternOrEmpty("0.123"));
        assertFalse(matchesCurrencyPatternOrEmpty("1."));
    }

    @Test
    public void matchesQuantityPattern_whenValid_returnsTrue() {
        assertTrue(matchesQuantityPatternOrEmpty(null));
        assertTrue(matchesQuantityPatternOrEmpty(""));
        assertTrue(matchesQuantityPatternOrEmpty("10.1"));
        assertTrue(matchesQuantityPatternOrEmpty("01"));
        assertTrue(matchesQuantityPatternOrEmpty("500000000000"));
        assertTrue(matchesQuantityPatternOrEmpty("100"));
    }

    @Test
    public void matchesQuantityPattern_whenInvalid_returnsFalse() {
        assertFalse(matchesQuantityPatternOrEmpty("1.23"));
        assertFalse(matchesQuantityPatternOrEmpty(".99"));
        assertFalse(matchesQuantityPatternOrEmpty("0.123"));
        assertFalse(matchesQuantityPatternOrEmpty("1."));
        assertFalse(matchesQuantityPatternOrEmpty("-5"));
    }

    @Test
    public void isLineItemListValid_whenEmptyList_returnsTrue() {
        assertTrue(isLineItemListValid(new ArrayList<LineItem>(), "USD"));
    }

    @Test
    public void isLineItemListValid_whenNull_returnsFalse() {
        assertFalse(isLineItemListValid(null, "JPY"));
    }

    @Test
    public void isLineItemListValid_whenEmptyCurrencyCode_returnsFalse() {
        assertFalse(isLineItemListValid(new ArrayList<LineItem>(), ""));
    }

    @Test
    public void isLineItemListValid_whenInvalidCurrencyCode_returnsFalse() {
        assertFalse(isLineItemListValid(new ArrayList<LineItem>(), "fakebucks"));
    }

    @Test
    public void isLineItemListValid_whenOneOrZeroTaxItems_returnsTrue() {
        Locale.setDefault(Locale.US);
        LineItem item0 = new LineItemBuilder().setTotalPrice(1000L).build();
        LineItem item1 = new LineItemBuilder().setTotalPrice(2000L)
                .setRole(LineItem.Role.TAX).build();


        List<LineItem> noTaxList = new ArrayList<>();
        List<LineItem> oneTaxList = new ArrayList<>();
        noTaxList.add(item0);
        oneTaxList.add(item0);
        oneTaxList.add(item1);

        assertTrue(isLineItemListValid(noTaxList, "USD"));
        assertTrue(isLineItemListValid(oneTaxList, "USD"));
    }

    @Test
    public void isLineItemListValid_whenTwoTaxItems_returnsFalse() {
        Locale.setDefault(Locale.US);

        LineItem item0 = LineItem.newBuilder().setCurrencyCode("USD")
                .setRole(LineItem.Role.REGULAR).build();
        LineItem item1 = LineItem.newBuilder().setCurrencyCode("USD")
                .setRole(LineItem.Role.TAX).build();
        LineItem item2 = LineItem.newBuilder().setCurrencyCode("USD")
                .setRole(LineItem.Role.TAX).build();
        LineItem item3 = LineItem.newBuilder().setCurrencyCode("USD")
                .setRole(LineItem.Role.REGULAR).build();

        List<LineItem> tooMuchTaxList = new ArrayList<>();
        tooMuchTaxList.add(item0);
        tooMuchTaxList.add(item1);
        tooMuchTaxList.add(item2);
        tooMuchTaxList.add(item3);

        assertFalse(isLineItemListValid(tooMuchTaxList, "USD"));
    }

    @Test
    public void isLineItemListValid_withOneBadItem_returnsFalse() {
        LineItem badItem = LineItem.newBuilder().setTotalPrice("10.999")
                .setCurrencyCode("USD").build();
        LineItem goodItem0 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();
        LineItem goodItem1 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();

        List<LineItem> oneBadAppleList = new ArrayList<>();
        oneBadAppleList.add(goodItem0);
        oneBadAppleList.add(badItem);
        oneBadAppleList.add(goodItem1);

        assertFalse(isLineItemListValid(oneBadAppleList, "USD"));
    }

    @Test
    public void isLineItemListValid_whenOneItemHasNoCurrency_returnsFalse() {
        LineItem badItem = LineItem.newBuilder().setTotalPrice("10.99").build();
        LineItem goodItem0 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();
        LineItem goodItem1 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();

        List<LineItem> mixedList = new ArrayList<>();
        mixedList.add(goodItem0);
        mixedList.add(badItem);
        mixedList.add(goodItem1);
        assertFalse(isLineItemListValid(mixedList, "USD"));

        List<LineItem> badItemFirstList = new ArrayList<>();
        badItemFirstList.add(badItem);
        badItemFirstList.add(goodItem0);
        badItemFirstList.add(goodItem1);
        assertFalse(isLineItemListValid(badItemFirstList, "USD"));
    }

    @Test
    public void isLineItemListValid_whenMixedCurrencies_returnsFalse() {
        LineItem euroItem = LineItem.newBuilder().setTotalPrice("10.99")
                .setCurrencyCode("EUR").build();
        LineItem dollarItem = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();
        LineItem yenItem = LineItem.newBuilder().setTotalPrice("1000")
                .setCurrencyCode("JPY").build();

        List<LineItem> mixedList = new ArrayList<>();
        mixedList.add(euroItem);
        mixedList.add(dollarItem);
        mixedList.add(yenItem);
        assertFalse(isLineItemListValid(mixedList, "EUR"));
    }

    @Test
    public void isLineItemValid_whenNoFieldsEntered_returnsTrue() {
        assertTrue(isLineItemValid(LineItem.newBuilder().build()));
    }

    @Test
    public void isLineItemValid_whenNull_returnsFalse() {
        assertFalse(isLineItemValid(null));
    }

    @Test
    public void isLineItemValid_withAllNumericFieldsCorrect_returnsTrue() {
        // Note that we don't assert that unitPrice * quantity ==  totalPrice
        assertTrue(isLineItemValid(LineItem.newBuilder()
                .setTotalPrice("10.00")
                .setQuantity("1.3")
                .setUnitPrice("1.50")
                .build()));
    }

    @Test
    public void isLineItemValid_whenJustOneIncorrectField_returnsFalse() {
        assertFalse(isLineItemValid(LineItem.newBuilder()
                .setTotalPrice("10.999")
                .setQuantity("1.3")
                .setUnitPrice("1.50")
                .build()));

        assertFalse(isLineItemValid(LineItem.newBuilder()
                .setTotalPrice("10.99")
                .setQuantity("1.33")
                .setUnitPrice("1.50")
                .build()));

        assertFalse(isLineItemValid(LineItem.newBuilder()
                .setTotalPrice("10.99")
                .setQuantity("1.3")
                .setUnitPrice(".50")
                .build()));
    }

    @Test
    public void isLineItemValid_withOneCorrectFieldAndOthersNull_returnsTrue() {
        assertTrue(isLineItemValid(LineItem.newBuilder()
                .setTotalPrice("10.00")
                .build()));
    }

    @Test
    public void getPriceString_whenCurrencyWithDecimals_returnsExpectedValue() {
        String priceString = getPriceString(100L, Currency.getInstance("USD"));
        assertEquals("1.00", priceString);
        assertTrue(matchesCurrencyPatternOrEmpty(priceString));

        String littlePrice = getPriceString(8L, Currency.getInstance("EUR"));
        assertEquals("0.08", littlePrice);
        assertTrue(matchesCurrencyPatternOrEmpty(littlePrice));

        String bigPrice = getPriceString(20000000L, Currency.getInstance("GBP"));
        assertEquals("200000.00", bigPrice);
        assertTrue(matchesCurrencyPatternOrEmpty(bigPrice));
    }

    @Test
    public void getPriceString_whenCurrencyWithoutDecimals_returnsExpectedValue() {
        String priceString = getPriceString(250L, Currency.getInstance("JPY"));
        assertEquals("250", priceString);
        assertTrue(matchesCurrencyPatternOrEmpty(priceString));

        String bigPrice = getPriceString(250000L, Currency.getInstance("KRW"));
        assertEquals("250000", bigPrice);
        assertTrue(matchesCurrencyPatternOrEmpty(bigPrice));

        String littlePrice = getPriceString(7L, Currency.getInstance("CLP"));
        assertEquals("7", littlePrice);
        assertTrue(matchesCurrencyPatternOrEmpty(littlePrice));
    }

    @Test
    public void getPriceString_whenNoCurrencyProvided_usesDefault() {
        Locale.setDefault(Locale.US);

        String dollarString = getPriceString(499L);
        assertEquals("4.99", dollarString);
        assertTrue(matchesCurrencyPatternOrEmpty(dollarString));

        Locale.setDefault(Locale.JAPAN);

        String yenString = getPriceString(499L);
        assertEquals("499", yenString);
        assertTrue(matchesCurrencyPatternOrEmpty(yenString));
    }

    @Test
    public void getPriceLong_whenDecimalValueGiven_correctlyParsesResult() {
        assertEquals(Long.valueOf(1000L), getPriceLong("10.00", Currency.getInstance("USD")));
        assertEquals(Long.valueOf(55555555L),
                getPriceLong("555555.55", Currency.getInstance("EUR")));
        assertEquals(Long.valueOf(99L), getPriceLong("0.99", Currency.getInstance("AUD")));
    }

    @Test
    public void getPriceLong_whenNonDecimalValueGivenInNonDecimalCurrency_correctlyParsesResult() {
        assertEquals(Long.valueOf(999L), getPriceLong("999", Currency.getInstance("JPY")));
        assertEquals(Long.valueOf(1L), getPriceLong("1", Currency.getInstance("KRW")));
    }

    @Test
    public void getPriceLong_whenNonDecimalValueGivenInDecimalCurrency_correctlyMovesUpResult() {
        assertEquals(Long.valueOf(1000L), getPriceLong("10", Currency.getInstance("USD")));
        // The Rial Omani has 3 decimal places
        assertEquals(Long.valueOf(30000L), getPriceLong("30", Currency.getInstance("OMR")));
    }

    @Test(expected = NumberFormatException.class)
    public void getPriceLong_whenDecimalValueInNonDecimalCurrency_throwsNumberFormatException() {
        getPriceLong("100.00", Currency.getInstance("JPY"));
        fail("Should throw NumberFormatException when trying to convert decimal amounts of Yen");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPriceLong_whenPriceStringHasAlphaCharacters_throwsIllegalArgumentException() {
        getPriceLong("55 bucks", Currency.getInstance("USD"));
        fail("Should throw IllegalArgumentException when trying to convert illegal characters");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPriceLong_whenPriceStringHasNoLeadingZero_throwsIllegalArgumentException() {
        getPriceLong(".88", Currency.getInstance("USD"));
        fail("Should throw IllegalArgumentException when trying to parse bad Strings");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPriceLong_whenPriceStringHasSeparators_throwsIllegalArgumentException() {
        getPriceLong("1,000", Currency.getInstance("USD"));
        fail("Should throw IllegalArgumentException when trying to parse bad Strings");
    }

    @Test
    public void getPriceLong_whenPriceStringIsNegative_correctlyParsesValue() {
        assertEquals(Long.valueOf(-15L), getPriceLong("-15", Currency.getInstance("JPY")));
        assertEquals(Long.valueOf(-1500L), getPriceLong("-15", Currency.getInstance("USD")));
        assertEquals(Long.valueOf(-9999L), getPriceLong("-99.99", Currency.getInstance("EUR")));
    }

    @Test
    public void getPriceLong_whenPriceStringIsEmpty_returnsNull() {
        assertNull(getPriceLong("", Currency.getInstance("USD")));
        assertNull(getPriceLong(null, Currency.getInstance("KRW")));
    }

    @Test
    public void getTotalPriceString_forGroupOfStandardLineItemsInUsd_returnsExpectedValue() {
        Locale.setDefault(Locale.US);
        LineItem item1 = new LineItemBuilder().setTotalPrice(1000L).build();
        LineItem item2 = new LineItemBuilder().setTotalPrice(2000L).build();
        LineItem item3 = new LineItemBuilder().setTotalPrice(3000L).build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        assertEquals("60.00", getTotalPriceString(items, Currency.getInstance("USD")));
    }

    @Test
    public void getTotalPriceString_forGroupOfStandardLineItemsInKrw_returnsExpectedValue() {
        Locale.setDefault(Locale.KOREA);
        LineItem item1 = new LineItemBuilder().setTotalPrice(1000L).build();
        LineItem item2 = new LineItemBuilder().setTotalPrice(2000L).build();
        LineItem item3 = new LineItemBuilder().setTotalPrice(3000L).build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        assertEquals("6000", getTotalPriceString(items, Currency.getInstance("KRW")));
    }

    @Test
    public void getTotalPriceString_whenOneItemHasNoPrice_returnsExpectedValue() {
        Locale.setDefault(Locale.US);
        LineItem item1 = new LineItemBuilder().setTotalPrice(1000L).build();
        LineItem item2 = new LineItemBuilder().build();
        LineItem item3 = new LineItemBuilder().setTotalPrice(3000L).build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        assertEquals("40.00", getTotalPriceString(items, Currency.getInstance("USD")));
    }

    @Test
    public void getTotalPriceString_whenNoItemHasPrice_returnsEmptyString() {
        Locale.setDefault(Locale.CANADA);
        LineItem item1 = new LineItemBuilder().build();
        LineItem item2 = new LineItemBuilder().build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        assertEquals("", getTotalPriceString(items, Currency.getInstance("CAD")));
    }

    @Test
    public void getTotalPriceString_whenEmptyList_returnsEmptyString() {
        assertEquals("", getTotalPriceString(
                new ArrayList<LineItem>(), Currency.getInstance("OMR")));
    }

    @Test
    public void getCurrencyByCodeOrDefault_forValidCurrencyCode_returnsCorrectCurrency() {
        Currency foundCurrency = getCurrencyByCodeOrDefault("CAD");
        assertEquals(Currency.getInstance(Locale.CANADA), foundCurrency);
    }

    @Test
    public void getCurrencyByCodeOrDefault_forNull_returnsDefault() {
        Locale.setDefault(Locale.KOREA);
        assertEquals(Currency.getInstance(Locale.KOREA),
                getCurrencyByCodeOrDefault(null));
    }

    @Test
    public void getCurrencyByCodeOrDefault_forInvalid_returnsDefault() {
        Locale.setDefault(Locale.UK);
        assertEquals(Currency.getInstance(Locale.UK),
                getCurrencyByCodeOrDefault("tea and crumpets"));
    }

    @Test
    public void getCurrencyByCodeOrDefault_forLowercase_stillReturnsCorrectCurrency() {
        assertEquals(Currency.getInstance(Locale.US),
                getCurrencyByCodeOrDefault("usd"));
    }
}

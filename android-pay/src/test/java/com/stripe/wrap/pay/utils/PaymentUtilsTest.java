package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;

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
import static com.stripe.wrap.pay.utils.PaymentUtils.validateLineItemList;
import static com.stripe.wrap.pay.utils.PaymentUtils.searchLineItemForErrors;
import static com.stripe.wrap.pay.utils.PaymentUtils.matchesCurrencyPatternOrEmpty;
import static com.stripe.wrap.pay.utils.PaymentUtils.matchesQuantityPatternOrEmpty;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
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
    public void validateLineItemList_whenEmptyList_passes() {
        try {
            validateLineItemList(new ArrayList<LineItem>(), "USD");
        } catch (CartContentException cartEx) {
            fail("Unexpected exception: " + cartEx.getMessage());
        }
    }

    @Test
    public void validateLineItemList_whenNull_passes() {
        try {
            validateLineItemList(null, "JPY");
        } catch (CartContentException cartEx) {
            fail("Unexpected exception: " + cartEx.getMessage());
        }
    }

    @Test
    public void validateLineItemList_whenEmptyCurrencyCode_throwsExpectedException() {
        try {
            validateLineItemList(new ArrayList<LineItem>(), "");
            fail("Cannot validate cart without currency.");
        } catch (CartContentException cartEx) {
            assertEquals(CartContentException.CART_CURRENCY, cartEx.getErrorType());
            assertEquals("Cart does not have a valid currency code. " +
                    "[empty] was used, but not recognized.", cartEx.getMessage());
            assertNull(cartEx.getLineItem());
        }
    }

    @Test
    public void validateLineItemList_whenInvalidCurrencyCode_throwsExpectedException() {
        try {
            validateLineItemList(new ArrayList<LineItem>(), "fakebucks");
            fail("Cannot validate cart without currency.");
        } catch (CartContentException cartEx) {
            assertEquals(CartContentException.CART_CURRENCY, cartEx.getErrorType());
            assertEquals("Cart does not have a valid currency code. " +
                    "fakebucks was used, but not recognized.", cartEx.getMessage());
            assertNull(cartEx.getLineItem());
        }
    }

    @Test
    public void validateLineItems_whenOneOrZeroTaxItems_passes() {
        Locale.setDefault(Locale.US);
        LineItem item0 = new LineItemBuilder().setTotalPrice(1000L).build();
        LineItem item1 = new LineItemBuilder().setTotalPrice(2000L)
                .setRole(LineItem.Role.TAX).build();


        List<LineItem> noTaxList = new ArrayList<>();
        List<LineItem> oneTaxList = new ArrayList<>();
        noTaxList.add(item0);
        oneTaxList.add(item0);
        oneTaxList.add(item1);

        try {
            validateLineItemList(noTaxList, "USD");
            validateLineItemList(oneTaxList, "USD");
        } catch (CartContentException cartEx) {
            fail("Unexpected exception: " + cartEx.getMessage());
        }
    }

    @Test
    public void validateLineItems_whenTwoTaxItems_throwsExpectedException() {
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

        try {
            validateLineItemList(tooMuchTaxList, "USD");
            fail("Line item list with two TAX items should not pass.");
        } catch (CartContentException cartEx) {
            assertEquals(CartContentException.DUPLICATE_TAX, cartEx.getErrorType());
            assertEquals("A cart may only have one item with a role of " +
                    "LineItem.Role.TAX, but more than one was found.",
                    cartEx.getMessage());
            assertEquals(item2, cartEx.getLineItem());
        }
    }

    @Test
    public void validateLineItemList_withOneBadItem_throwsExpectedException() {
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

        try {
            validateLineItemList(oneBadAppleList, "USD");
            fail("validateLineItemList should not pass when one item is incorrectly formatted.");
        } catch (CartContentException cartEx) {
            assertEquals(CartContentException.LINE_ITEM_PRICE, cartEx.getErrorType());
            assertEquals(getExpectedErrorStringForPrice("10.999"), cartEx.getMessage());
            assertEquals(badItem, cartEx.getLineItem());
        }
    }

    @Test
    public void validateLineItemList_whenOneItemHasNoCurrency_throwsExpectedException() {
        LineItem badItem = LineItem.newBuilder().setTotalPrice("10.99").build();
        LineItem goodItem0 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();
        LineItem goodItem1 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();

        List<LineItem> mixedList = new ArrayList<>();
        mixedList.add(goodItem0);
        mixedList.add(badItem);
        mixedList.add(goodItem1);
        try {
            validateLineItemList(mixedList, "USD");
            fail("Line item without currency should not validate");
        } catch (CartContentException cartEx) {
            assertEquals(CartContentException.LINE_ITEM_CURRENCY, cartEx.getErrorType());
            assertEquals("Line item currency of [empty] does not match cart currency of USD.",
                    cartEx.getMessage());
            assertEquals(badItem, cartEx.getLineItem());
        }

        List<LineItem> badItemFirstList = new ArrayList<>();
        badItemFirstList.add(badItem);
        badItemFirstList.add(goodItem0);
        badItemFirstList.add(goodItem1);
        try {
            validateLineItemList(badItemFirstList, "USD");
            fail("Line item without currency should not validate");
        } catch (CartContentException cartEx) {
            assertEquals(CartContentException.LINE_ITEM_CURRENCY, cartEx.getErrorType());
            assertEquals("Line item currency of [empty] does not match cart currency of USD.",
                    cartEx.getMessage());
            assertEquals(badItem, cartEx.getLineItem());
        }
    }

    @Test
    public void validateLineItemList_whenMixedCurrencies_throwsExpectedException() {
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

        try {
            validateLineItemList(mixedList, "EUR");
            fail("List with mixed currencies should not validate.");
        } catch (CartContentException cartEx) {
            assertEquals(CartContentException.LINE_ITEM_CURRENCY, cartEx.getErrorType());
            assertEquals("Line item currency of USD does not match cart currency of EUR.",
                    cartEx.getMessage());
            assertEquals(dollarItem, cartEx.getLineItem());
        }
    }

    @Test
    public void searchLineItemForErrors_whenNoFieldsEntered_returnsNull() {
        assertNull(searchLineItemForErrors(LineItem.newBuilder().build()));
    }

    @Test
    public void searchLineItemForErrors_whenNull_returnsNull() {
        assertNull(searchLineItemForErrors(null));
    }

    @Test
    public void searchLineItemForErrors_withAllNumericFieldsCorrect_returnsNull() {
        // Note that we don't assert that unitPrice * quantity ==  totalPrice
        assertNull(searchLineItemForErrors(LineItem.newBuilder()
                .setTotalPrice("10.00")
                .setQuantity("1.3")
                .setUnitPrice("1.50")
                .build()));
    }

    @Test
    public void searchLineItemForErrors_whenJustOneIncorrectField_returnsExpectedError() {
        LineItem badTotalPriceItem = LineItem.newBuilder()
                .setTotalPrice("10.999")
                .setQuantity("1.3")
                .setUnitPrice("1.50")
                .build();
        CartContentException totalPriceException = searchLineItemForErrors(badTotalPriceItem);
        assertNotNull(totalPriceException);
        assertEquals(CartContentException.LINE_ITEM_PRICE, totalPriceException.getErrorType());
        assertEquals(getExpectedErrorStringForPrice("10.999"), totalPriceException.getMessage());
        assertEquals(badTotalPriceItem, totalPriceException.getLineItem());

        LineItem badQuantityItem = LineItem.newBuilder()
                .setTotalPrice("10.99")
                .setQuantity("1.33")
                .setUnitPrice("1.50")
                .build();
        CartContentException quantityException = searchLineItemForErrors(badQuantityItem);
        assertNotNull(quantityException);
        assertEquals(CartContentException.LINE_ITEM_QUANTITY, quantityException.getErrorType());
        assertEquals(getExpectedErrorStringForQuantity("1.33"), quantityException.getMessage());
        assertEquals(badQuantityItem, quantityException.getLineItem());

        LineItem badUnitPriceItem = LineItem.newBuilder()
                .setTotalPrice("10.99")
                .setQuantity("1.3")
                .setUnitPrice(".50")
                .build();
        CartContentException unitPriceException = searchLineItemForErrors(badUnitPriceItem);
        assertNotNull(unitPriceException);
        assertEquals(CartContentException.LINE_ITEM_PRICE, unitPriceException.getErrorType());
        assertEquals(getExpectedErrorStringForPrice(".50"), unitPriceException.getMessage());
        assertEquals(badUnitPriceItem, unitPriceException.getLineItem());
    }

    @Test
    public void searchLineItemForErrors_withOneCorrectFieldAndOthersNull_returnsNull() {
        assertNull(searchLineItemForErrors(LineItem.newBuilder()
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

    // ************ Test Helper Methods ************ //

    @NonNull
    private static String getExpectedErrorStringForPrice(String price) {
        return String.format(Locale.ENGLISH,
                "Invalid price string: %s does not match required pattern of " +
                        "\"^-?[0-9]+(\\.[0-9][0-9])?\"",
                price);
    }

    @NonNull
    private static String getExpectedErrorStringForQuantity(String quantity) {
        return String.format(Locale.ENGLISH,
                "Invalid quantity string: %s does not match required pattern of " +
                        "\"[0-9]+(\\.[0-9])?\"",
                quantity);
    }
}

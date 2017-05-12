package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;

import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.stripe.wrap.pay.testutils.AssertUtils.assertEmpty;
import static com.stripe.wrap.pay.utils.PaymentUtils.getPriceLong;
import static com.stripe.wrap.pay.utils.PaymentUtils.getPriceString;
import static com.stripe.wrap.pay.utils.PaymentUtils.getStripeIsReadyToPayRequest;
import static com.stripe.wrap.pay.utils.PaymentUtils.getTotalPrice;
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
    public void validateLineItemList_whenEmptyList_returnsNoErrors() {
        assertEmpty(validateLineItemList(new ArrayList<LineItem>(), "USD"));
    }

    @Test
    public void validateLineItemList_whenNull_returnsNoErrors() {
        assertEmpty(validateLineItemList(null, "JPY"));
    }

    @Test
    public void validateLineItemList_whenEmptyCurrencyCode_hasExpectedCartError() {
        List<CartError> errors = validateLineItemList(new ArrayList<LineItem>(), "");

        assertEquals(1, errors.size());
        CartError error = errors.get(0);
        assertEquals(CartError.CART_CURRENCY, error.getErrorType());
        assertEquals("Cart does not have a valid currency code. " +
                "[empty] was used, but not recognized.", error.getMessage());
        assertNull(error.getLineItem());
    }

    @Test
    public void validateLineItemList_whenInvalidCurrencyCode_hasExpectedCartError() {
        List<CartError> errors = validateLineItemList(new ArrayList<LineItem>(), "fakebucks");

        assertEquals(1, errors.size());
        CartError error = errors.get(0);
        assertEquals(CartError.CART_CURRENCY, error.getErrorType());
        assertEquals("Cart does not have a valid currency code. " +
                "fakebucks was used, but not recognized.", error.getMessage());
        assertNull(error.getLineItem());
    }

    @Test
    public void validateLineItems_whenOneOrZeroTaxItems_returnsNoErrors() {
        Locale.setDefault(Locale.US);
        LineItem item0 = new LineItemBuilder("USD").setTotalPrice(1000L).build();
        LineItem item1 = new LineItemBuilder("USD").setTotalPrice(2000L)
                .setRole(LineItem.Role.TAX).build();


        List<LineItem> noTaxList = new ArrayList<>();
        List<LineItem> oneTaxList = new ArrayList<>();
        noTaxList.add(item0);
        oneTaxList.add(item0);
        oneTaxList.add(item1);

        assertEmpty(validateLineItemList(noTaxList, "USD"));
        assertEmpty(validateLineItemList(oneTaxList, "USD"));
    }

    @Test
    public void validateLineItems_whenTwoTaxItems_hasExpectedCartError() {
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

        List<CartError> cartErrors = validateLineItemList(tooMuchTaxList, "USD");
        assertEquals(1, cartErrors.size());
        CartError error = cartErrors.get(0);
        assertEquals(CartError.DUPLICATE_TAX, error.getErrorType());
        assertEquals("A cart may only have one item with a role of " +
                        "LineItem.Role.TAX, but more than one was found.",
                error.getMessage());
        assertEquals(item2, error.getLineItem());
    }

    @Test
    public void validateLineItemList_withOneBadItem_hasExpectedCartError() {
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

        List<CartError> cartErrors = validateLineItemList(oneBadAppleList, "USD");
        assertEquals(1, cartErrors.size());
        CartError error = cartErrors.get(0);
        assertEquals(CartError.LINE_ITEM_PRICE, error.getErrorType());
        assertEquals(getExpectedErrorStringForPrice("10.999"), error.getMessage());
        assertEquals(badItem, error.getLineItem());
    }

    @Test
    public void validateLineItemList_whenOneItemHasNoCurrency_hasExpectedCartError() {
        LineItem badItem = LineItem.newBuilder().setTotalPrice("10.99").build();
        LineItem goodItem0 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();
        LineItem goodItem1 = LineItem.newBuilder().setTotalPrice("10.00")
                .setCurrencyCode("USD").build();

        List<LineItem> mixedList = new ArrayList<>();
        mixedList.add(goodItem0);
        mixedList.add(badItem);
        mixedList.add(goodItem1);

        List<CartError> cartErrors = validateLineItemList(mixedList, "USD");
        assertEquals(1, cartErrors.size());
        CartError error = cartErrors.get(0);
        assertEquals(CartError.LINE_ITEM_CURRENCY, error.getErrorType());
        assertEquals("Line item currency of [empty] does not match cart currency of USD.",
                error.getMessage());
        assertEquals(badItem, error.getLineItem());
    }

    @Test
    public void validateLineItemList_whenMixedCurrencies_hasExpectedCartErrors() {
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

        List<CartError> cartErrors = validateLineItemList(mixedList, "EUR");
        assertEquals(2, cartErrors.size());

        CartError dollarError = cartErrors.get(0);
        assertEquals(CartError.LINE_ITEM_CURRENCY, dollarError.getErrorType());
        assertEquals("Line item currency of USD does not match cart currency of EUR.",
                dollarError.getMessage());
        assertEquals(dollarItem, dollarError.getLineItem());

        CartError yenError = cartErrors.get(1);
        assertEquals(CartError.LINE_ITEM_CURRENCY, yenError.getErrorType());
        assertEquals("Line item currency of JPY does not match cart currency of EUR.",
                yenError.getMessage());
        assertEquals(yenItem, yenError.getLineItem());
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
        CartError totalPriceError = searchLineItemForErrors(badTotalPriceItem);
        assertNotNull(totalPriceError);
        assertEquals(CartError.LINE_ITEM_PRICE, totalPriceError.getErrorType());
        assertEquals(getExpectedErrorStringForPrice("10.999"), totalPriceError.getMessage());
        assertEquals(badTotalPriceItem, totalPriceError.getLineItem());

        LineItem badQuantityItem = LineItem.newBuilder()
                .setTotalPrice("10.99")
                .setQuantity("1.33")
                .setUnitPrice("1.50")
                .build();
        CartError quantityError = searchLineItemForErrors(badQuantityItem);
        assertNotNull(quantityError);
        assertEquals(CartError.LINE_ITEM_QUANTITY, quantityError.getErrorType());
        assertEquals(getExpectedErrorStringForQuantity("1.33"), quantityError.getMessage());
        assertEquals(badQuantityItem, quantityError.getLineItem());

        LineItem badUnitPriceItem = LineItem.newBuilder()
                .setTotalPrice("10.99")
                .setQuantity("1.3")
                .setUnitPrice(".50")
                .build();
        CartError unitPriceError = searchLineItemForErrors(badUnitPriceItem);
        assertNotNull(unitPriceError);
        assertEquals(CartError.LINE_ITEM_PRICE, unitPriceError.getErrorType());
        assertEquals(getExpectedErrorStringForPrice(".50"), unitPriceError.getMessage());
        assertEquals(badUnitPriceItem, unitPriceError.getLineItem());
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
    public void getTotalPrice_forGroupOfStandardLineItemsInUsd_returnsExpectedValue() {
        Locale.setDefault(Locale.US);
        LineItem item1 = new LineItemBuilder("USD").setTotalPrice(1000L).build();
        LineItem item2 = new LineItemBuilder("USD").setTotalPrice(2000L).build();
        LineItem item3 = new LineItemBuilder("USD").setTotalPrice(3000L).build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        assertEquals(Long.valueOf(6000L), getTotalPrice(items, Currency.getInstance("USD")));
    }

    @Test
    public void getTotalPrice_forGroupOfStandardLineItemsInKrw_returnsExpectedValue() {
        Locale.setDefault(Locale.KOREA);
        LineItem item1 = new LineItemBuilder("KRW").setTotalPrice(1000L).build();
        LineItem item2 = new LineItemBuilder("KRW").setTotalPrice(2000L).build();
        LineItem item3 = new LineItemBuilder("KRW").setTotalPrice(3000L).build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        assertEquals(Long.valueOf(6000L), getTotalPrice(items, Currency.getInstance("KRW")));
    }

    @Test
    public void getTotalPrice_whenOneItemHasNoPrice_returnsExpectedValue() {
        Locale.setDefault(Locale.US);
        LineItem item1 = new LineItemBuilder("USD").setTotalPrice(1000L).build();
        LineItem item2 = new LineItemBuilder("USD").build();
        LineItem item3 = new LineItemBuilder("USD").setTotalPrice(3000L).build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        assertEquals(Long.valueOf(4000L), getTotalPrice(items, Currency.getInstance("USD")));
    }

    @Test
    public void getTotalPrice_whenNoItemHasPrice_returnsZero() {
        Locale.setDefault(Locale.CANADA);
        LineItem item1 = new LineItemBuilder("CAD").build();
        LineItem item2 = new LineItemBuilder("CAD").build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        assertEquals(Long.valueOf(0L), getTotalPrice(items, Currency.getInstance("CAD")));
    }

    @Test
    public void getTotalPrice_whenOneItemHasInvalidCurrency_returnsNull() {
        LineItem item1 = new LineItemBuilder("USD").setTotalPrice(100L).build();
        LineItem item2 = new LineItemBuilder("CAD").setTotalPrice(100L).build();
        List<LineItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        assertNull(getTotalPrice(items, Currency.getInstance("USD")));
    }

    @Test
    public void getTotalPrice_whenEmptyList_returnsZero() {
        assertEquals(Long.valueOf(0L), getTotalPrice(
                new ArrayList<LineItem>(), Currency.getInstance("OMR")));
    }

    @Test
    public void getIsReadyToPayRequest_hasExpectedCardNetworks() {
        IsReadyToPayRequest isReadyToPayRequest = getStripeIsReadyToPayRequest();
        Set<Integer> allowedNetworks = new HashSet<>();
        allowedNetworks.addAll(isReadyToPayRequest.getAllowedCardNetworks());

        assertEquals(5, allowedNetworks.size());
        assertTrue(allowedNetworks.contains(WalletConstants.CardNetwork.VISA));
        assertTrue(allowedNetworks.contains(WalletConstants.CardNetwork.AMEX));
        assertTrue(allowedNetworks.contains(WalletConstants.CardNetwork.MASTERCARD));
        assertTrue(allowedNetworks.contains(WalletConstants.CardNetwork.JCB));
        assertTrue(allowedNetworks.contains(WalletConstants.CardNetwork.DISCOVER));
    }

    @Test
    public void removeErrorType_whenListContainsErrorsOfType_returnsListWithoutThoseErrors() {
        final CartError error1 = new CartError(CartError.DUPLICATE_TAX, "Dupe Tax");
        final CartError error2 = new CartError(CartError.LINE_ITEM_CURRENCY, "Bad line item");
        final CartError error3 = new CartError(CartError.LINE_ITEM_PRICE, "Bad price on line item");
        final CartError error4 = new CartError(CartError.LINE_ITEM_QUANTITY, "Bad quantity");
        final CartError error5 = new CartError(CartError.DUPLICATE_TAX, "Dupe Tax");

        List<CartError> errorList = new ArrayList<CartError>() {{
            add(error1);
            add(error2);
            add(error3);
            add(error4);
            add(error5);
        }};

        List<CartError> filteredErrorList = PaymentUtils.removeErrorType(errorList,
                CartError.DUPLICATE_TAX);
        assertEquals(3, filteredErrorList.size());
        assertEquals(CartError.LINE_ITEM_CURRENCY, filteredErrorList.get(0).getErrorType());
        assertEquals(CartError.LINE_ITEM_PRICE, filteredErrorList.get(1).getErrorType());
        assertEquals(CartError.LINE_ITEM_QUANTITY, filteredErrorList.get(2).getErrorType());
    }

    @Test
    public void removeErrorType_whenListHasNoErrorsOfType_returnsOriginalList() {
        final CartError error1 = new CartError(CartError.DUPLICATE_TAX, "Dupe Tax");
        final CartError error2 = new CartError(CartError.CART_CURRENCY, "Bad line item");

        List<CartError> errorList = new ArrayList<CartError>() {{
            add(error1);
            add(error2);
        }};

        List<CartError> filteredErrorList = PaymentUtils.removeErrorType(errorList,
                CartError.LINE_ITEM_CURRENCY);
        assertEquals(2, filteredErrorList.size());
        assertEquals(CartError.DUPLICATE_TAX, filteredErrorList.get(0).getErrorType());
        assertEquals(CartError.CART_CURRENCY, filteredErrorList.get(1).getErrorType());
    }

    @Test
    public void removeErrorType_onEmptyList_returnsEmptyList() {
        assertEmpty(PaymentUtils.removeErrorType(new ArrayList<CartError>(),
                CartError.LINE_ITEM_QUANTITY));
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

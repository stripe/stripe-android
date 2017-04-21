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

import static com.stripe.wrap.pay.utils.PaymentUtils.getPriceString;
import static com.stripe.wrap.pay.utils.PaymentUtils.isLineItemListValid;
import static com.stripe.wrap.pay.utils.PaymentUtils.isLineItemValid;
import static com.stripe.wrap.pay.utils.PaymentUtils.matchesCurrencyPatternOrEmpty;
import static com.stripe.wrap.pay.utils.PaymentUtils.matchesQuantityPatternOrEmpty;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
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
        assertTrue(isLineItemListValid(new ArrayList<LineItem>()));
    }

    @Test
    public void isLineItemListValid_whenNull_returnsFalse() {
        assertFalse(isLineItemListValid(null));
    }

    @Test
    public void isLineItemListValid_whenOneOrZeroTaxItems_returnsTrue() {
        LineItem item0 = LineItem.newBuilder().setRole(LineItem.Role.REGULAR).build();
        LineItem item1 = LineItem.newBuilder().setRole(LineItem.Role.TAX).build();

        List<LineItem> noTaxList = new ArrayList<>();
        List<LineItem> oneTaxList = new ArrayList<>();
        noTaxList.add(item0);
        oneTaxList.add(item0);
        oneTaxList.add(item1);

        assertTrue(isLineItemListValid(noTaxList));
        assertTrue(isLineItemListValid(oneTaxList));
    }

    @Test
    public void isLineItemListValid_whenTwoTaxItems_returnsFalse() {
        LineItem item0 = LineItem.newBuilder().setRole(LineItem.Role.REGULAR).build();
        LineItem item1 = LineItem.newBuilder().setRole(LineItem.Role.TAX).build();
        LineItem item2 = LineItem.newBuilder().setRole(LineItem.Role.TAX).build();
        LineItem item3 = LineItem.newBuilder().setRole(LineItem.Role.REGULAR).build();

        List<LineItem> tooMuchTaxList = new ArrayList<>();
        tooMuchTaxList.add(item0);
        tooMuchTaxList.add(item1);
        tooMuchTaxList.add(item2);
        tooMuchTaxList.add(item3);

        assertFalse(isLineItemListValid(tooMuchTaxList));
    }

    @Test
    public void isLineItemListValid_withOneBadItem_returnsFalse() {
        LineItem badItem = LineItem.newBuilder().setTotalPrice("10.999").build();
        LineItem goodItem0 = LineItem.newBuilder().setTotalPrice("10.00").build();
        LineItem goodItem1 = LineItem.newBuilder().setTotalPrice("10.00").build();

        List<LineItem> oneBadAppleList = new ArrayList<>();
        oneBadAppleList.add(goodItem0);
        oneBadAppleList.add(badItem);
        oneBadAppleList.add(goodItem1);

        assertFalse(isLineItemListValid(oneBadAppleList));
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
}

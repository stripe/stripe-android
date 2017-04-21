package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;

/**
 * Utility class for easily generating Android Pay items.
 */
public class PaymentUtils {

    /**
     * Checks whether or not the input String matches the regex required for Android Pay price
     * descriptions. This string should not include a currency symbol. For instance, ten USD would
     * be input as "10.00".
     *
     * @param priceString a String that may get displayed to the user
     * @return {@code true} if this string can be used as a price in a {@link Cart} or
     * a {@link LineItem}
     */
    public static boolean matchesCurrencyPatternOrEmpty(@Nullable String priceString) {
        if (TextUtils.isEmpty(priceString)) {
            return true;
        }
        Pattern pattern = Pattern.compile("^-?[0-9]+(\\.[0-9][0-9])?");
        return pattern.matcher(priceString).matches();
    }

    /**
     * Checks whether or not the input String matches the regex required for Android Pay quantity
     * descriptions. This string should not include a negative sign. It may include one number
     * after the decimal.
     *
     * @param quantityString a String that may get displayed to the user
     * @return {@code true} if this string can be used as a price in a {@link Cart} or
     * a {@link LineItem}
     */
    public static boolean matchesQuantityPatternOrEmpty(@Nullable String quantityString) {
        if (TextUtils.isEmpty(quantityString)) {
            return true;
        }
        Pattern pattern = Pattern.compile("[0-9]+(\\.[0-9])?");
        return pattern.matcher(quantityString).matches();

    }

    /**
     * Checks whether or not a list of {@link LineItem} objects is valid. A {@link Cart} may have
     * at most one item with a role of {@link LineItem.Role#TAX}.
     *
     * @param lineItems a list of {@link LineItem} objects
     * @return {@code true} if the list could be put into a {@link Cart}, false otherwise
     */
    public static boolean isLineItemListValid(List<LineItem> lineItems) {
        if (lineItems == null) {
            return false;
        }

        boolean hasTax = false;
        for (LineItem item : lineItems) {
            if (LineItem.Role.TAX == item.getRole()) {
                if (hasTax) {
                    return false;
                } else {
                    hasTax = true;
                }
            }

            if (!isLineItemValid(item)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether or not the fields of the input {@link LineItem} are valid, according to the
     * Android Pay specifications.
     *
     * @param lineItem a {@link LineItem} to check
     * @return {@code true} if this item is valid to be added to a {@link Cart}
     */
    public static boolean isLineItemValid(LineItem lineItem) {
        return lineItem != null
                && matchesCurrencyPatternOrEmpty(lineItem.getTotalPrice())
                && matchesQuantityPatternOrEmpty(lineItem.getQuantity())
                && matchesCurrencyPatternOrEmpty(lineItem.getUnitPrice());
    }

    /**
     * Converts an integer price in the lowest currency denomination to a Google string value.
     * The currency is assumed to be from {@link Locale#getDefault()}.
     *
     * @param price the price in the lowest available currency denomination
     * @return a String that can be used as an Android Pay price string
     */
    public static String getPriceString(long price) {
        return getPriceString(price, Currency.getInstance(Locale.getDefault()));
    }

    /**
     * Converts an integer price in the lowest currency denomination to a Google string value.
     * For instance (100, USD) -> "1.00", but (100, JPY) -> "100"
     * @param price the price in the lowest available currency denomination
     * @param currency the {@link Currency} used to determine how many digits after the decimal
     * @return a String that can be used as an Android Pay price string
     */
    public static String getPriceString(long price, @NonNull Currency currency) {

        int fractionDigits = currency.getDefaultFractionDigits();
        int totalLength = String.valueOf(price).length();
        StringBuilder builder = new StringBuilder();

        if (fractionDigits == 0) {
            for (int i = 0; i < totalLength; i++) {
                builder.append('#');
            }
            DecimalFormat noDecimalCurrencyFormat = new DecimalFormat(builder.toString());
            noDecimalCurrencyFormat.setCurrency(currency);
            return noDecimalCurrencyFormat.format(price);
        }

        int beforeDecimal = totalLength - fractionDigits;
        for (int i = 0; i < beforeDecimal; i++) {
            builder.append('#');
        }

        // So we display "0.55" instead of ".55"
        if (totalLength <= fractionDigits) {
            builder.append('0');
        }
        builder.append('.');
        for (int i = 0; i < fractionDigits; i++) {
            builder.append('0');
        }
        double modBreak = Math.pow(10, fractionDigits);
        double decimalPrice = price / modBreak;

        DecimalFormat decimalFormat = new DecimalFormat(builder.toString());
        decimalFormat.setCurrency(currency);

        return decimalFormat.format(decimalPrice);
    }
}

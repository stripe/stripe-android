package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.WalletConstants;

/**
 * Utility class for easily generating Android Pay items.
 */
public class PaymentUtils {

    static final String TAG = "Stripe:PaymentUtils";
    static final String CURRENCY_REGEX = "\"^-?[0-9]+(\\.[0-9][0-9])?\"";
    static final String QUANTITY_REGEX = "\"[0-9]+(\\.[0-9])?\"";

    static String getTotalPriceString(@NonNull List<LineItem> lineItems,
                                      @NonNull Currency currency) {
        Long totalPrice = null;
        for (LineItem lineItem : lineItems) {
            Long itemPrice = getPriceLong(lineItem.getTotalPrice(), currency);
            if (itemPrice != null) {
                if (totalPrice == null) {
                    totalPrice = itemPrice;
                } else {
                    totalPrice += itemPrice;
                }
            }
        }

        if (totalPrice == null) {
            return "";
        } else {
            return getPriceString(totalPrice, currency);
        }
    }

    @NonNull
    static Currency getCurrencyByCodeOrDefault(@Nullable String currencyCode) {
        Currency defaultCurrency = Currency.getInstance(Locale.getDefault());
        if (currencyCode == null) {
            return defaultCurrency;
        }
        try {
            return Currency.getInstance(currencyCode.toUpperCase());
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.w(TAG, String.format(Locale.ENGLISH,
                    "Could not create currency with code \"%s\". " +
                            "Using currency %s by default.",
                    currencyCode, defaultCurrency.getCurrencyCode()));
            return defaultCurrency;
        }
    }

    /**
     * Utility function to convert the already-valid price String obtained from a {@link LineItem}
     * into a {@link Long} value that can be used for calculations.
     *
     * @param price a price string that successfully passes
     *              {@link #matchesCurrencyPatternOrEmpty(String)}.
     *              If the input does not match this pattern
     *              an {@link IllegalArgumentException} is thrown.
     * @param currency the {@link Currency} to expect for this price value
     * @return {@code null} if the input is empty, otherwise a {@link Long} value representing
     *          the price in the lowest denomination of the input {@link Currency}
     */
    @Nullable
    static Long getPriceLong(@Nullable String price, @NonNull Currency currency) {
        if (TextUtils.isEmpty(price)) {
            return null;
        }

        if (!matchesCurrencyPatternOrEmpty(price)) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH,
                    "%s is not a valid price String for a LineItem", price));
        }

        int fractionDigits = currency.getDefaultFractionDigits();
        if (fractionDigits == 0) {
            return Long.parseLong(price);
        }


        // This is the case where the currency has a decimal, but our price does not.
        // For instance if the currency was USD and the price string was "2", we want to return
        // 200L, not 2L.
        if (!price.contains(".")) {
            long multiplier = (long) Math.pow(10, fractionDigits);
            long displayNumber = Long.parseLong(price);
            return displayNumber * multiplier;
        }

        String noDecimal = price.replace(".", "");
        return Long.parseLong(noDecimal);
    }

    /**
     * Checks whether or not the input String matches the regex required for Android Pay price
     * descriptions. This string should not include a currency symbol or separators.
     * For instance, one thousand USD would be input as "1000.00".
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
     * descriptions. This string should not include a negative sign or separators.
     * It may include one number after the decimal.
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
     * at most one item with a role of {@link LineItem.Role#TAX}. All items in a {@link Cart} must
     * have the same currency code, and it must match the input currency code
     *
     * @param lineItems a list of {@link LineItem} objects
     * @param currencyCode the currency code used to evaluate the list
     * @return {@code true} if the list could be put into a {@link Cart}, false otherwise
     */
    @NonNull
    public static List<CartError> validateLineItemList(
            List<LineItem> lineItems,
            @NonNull String currencyCode) {
        List<CartError> cartErrors = new ArrayList<>();
        if (lineItems == null) {
            return cartErrors;
        }

        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException illegalArgumentException) {
            cartErrors.add(new CartError(CartError.CART_CURRENCY,
                    String.format(Locale.ENGLISH,
                            "Cart does not have a valid currency code. " +
                                    "%s was used, but not recognized.",
                            TextUtils.isEmpty(currencyCode)
                                    ? "[empty]": currencyCode)));

        }

        boolean hasTax = false;
        for (LineItem item : lineItems) {
            if (!currencyCode.equals(item.getCurrencyCode())) {
                cartErrors.add(new CartError(
                        CartError.LINE_ITEM_CURRENCY,
                        String.format(Locale.ENGLISH,
                                "Line item currency of %s does not match cart currency of %s.",
                                TextUtils.isEmpty(item.getCurrencyCode())
                                        ? "[empty]": item.getCurrencyCode(),
                                currencyCode),
                        item));
            }

            if (LineItem.Role.TAX == item.getRole()) {
                if (hasTax) {
                    cartErrors.add(new CartError(
                            CartError.DUPLICATE_TAX,
                            "A cart may only have one item with a role of " +
                                    "LineItem.Role.TAX, but more than one was found.",
                            item));
                } else {
                    hasTax = true;
                }
            }

            CartError lineItemError = searchLineItemForErrors(item);
            if (lineItemError != null) {
                cartErrors.add(lineItemError);
            }
        }
        return cartErrors;
    }

    /**
     * Checks whether or not the fields of the input {@link LineItem} are valid, according to the
     * Android Pay specifications.
     *
     * @param lineItem a {@link LineItem} to check
     * @return {@code true} if this item is valid to be added to a {@link Cart}
     */
    public static CartError searchLineItemForErrors(LineItem lineItem) {
        if (lineItem == null) {
            return null;
        }

        if (!matchesCurrencyPatternOrEmpty(lineItem.getUnitPrice())) {
            return new CartError(CartError.LINE_ITEM_PRICE,
                    String.format(Locale.ENGLISH,
                            "Invalid price string: %s does not match required pattern of %s",
                            lineItem.getUnitPrice(),
                            CURRENCY_REGEX),
                    lineItem);
        }

        if (!matchesQuantityPatternOrEmpty(lineItem.getQuantity())) {
            return new CartError(CartError.LINE_ITEM_QUANTITY,
                    String.format(Locale.ENGLISH,
                            "Invalid quantity string: %s does not match required pattern of %s",
                            lineItem.getQuantity(),
                            QUANTITY_REGEX),
                    lineItem);
        }

        if (!matchesCurrencyPatternOrEmpty(lineItem.getTotalPrice())) {
            return new CartError(CartError.LINE_ITEM_PRICE,
                    String.format(Locale.ENGLISH,
                            "Invalid price string: %s does not match required pattern of %s",
                            lineItem.getTotalPrice(),
                            CURRENCY_REGEX),
                    lineItem);
        }

        return null;
    }

    /**
     * Converts an integer price in the lowest currency denomination to a Google string value.
     * The currency is assumed to be from {@link Locale#getDefault()}.
     *
     * @param price the price in the lowest available currency denomination
     * @return a String that can be used as an Android Pay price string
     */
    @NonNull
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
    @NonNull
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

    /**
     * Get an {@link IsReadyToPayRequest} that contains all of Stripe's accepted cards.
     *
     * @return an {@link IsReadyToPayRequest} that explicitly allows AmEx, Discover, JCB, VISA,
     * and Mastercard
     */
    public static IsReadyToPayRequest getStripeIsReadyToPayRequest() {
        return IsReadyToPayRequest.newBuilder()
                .addAllowedCardNetwork(WalletConstants.CardNetwork.AMEX)
                .addAllowedCardNetwork(WalletConstants.CardNetwork.DISCOVER)
                .addAllowedCardNetwork(WalletConstants.CardNetwork.JCB)
                .addAllowedCardNetwork(WalletConstants.CardNetwork.MASTERCARD)
                .addAllowedCardNetwork(WalletConstants.CardNetwork.VISA)
                .build();
    }
}

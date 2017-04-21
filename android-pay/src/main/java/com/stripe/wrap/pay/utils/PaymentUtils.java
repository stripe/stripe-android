package com.stripe.wrap.pay.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.List;
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
}

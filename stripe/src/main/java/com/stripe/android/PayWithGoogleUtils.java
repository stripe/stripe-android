package com.stripe.android;

import android.support.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;

/**
 * Public utility class for common Pay with Google-related tasks.
 */
public class PayWithGoogleUtils {

    /**
     * Converts an integer price in the lowest currency denomination to a Google string value.
     * For instance: (100L, USD) -> "1.00", but (100L, JPY) -> "100".
     * @param price the price in the lowest available currency denomination
     * @param currency the {@link Currency} used to determine how many digits after the decimal
     * @return a String that can be used as a Pay with Google price string
     */
    @NonNull
    public static String getPriceString(@NonNull long price, @NonNull Currency currency) {
        int fractionDigits = currency.getDefaultFractionDigits();
        int totalLength = String.valueOf(price).length();
        StringBuilder builder = new StringBuilder();

        if (fractionDigits == 0) {
            for (int i = 0; i < totalLength; i++) {
                builder.append('#');
            }
            DecimalFormat noDecimalCurrencyFormat = new DecimalFormat(builder.toString());
            noDecimalCurrencyFormat.setCurrency(currency);
            noDecimalCurrencyFormat.setGroupingUsed(false);
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

        // No matter the Locale, Android Pay requires a dot for the decimal separator.
        DecimalFormatSymbols symbolOverride = new DecimalFormatSymbols();
        symbolOverride.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat(builder.toString(), symbolOverride);
        decimalFormat.setCurrency(currency);
        decimalFormat.setGroupingUsed(false);

        return decimalFormat.format(decimalPrice);
    }
}

package com.stripe.samplestore;

import android.support.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.Currency;

/**
 * Class for utility functions.
 */
public class StoreUtils {

    static String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    static String getPriceString(long price, @Nullable Currency currency) {
        Currency displayCurrency = currency == null
                ? Currency.getInstance("USD")
                : currency;

        int fractionDigits = displayCurrency.getDefaultFractionDigits();
        int totalLength = String.valueOf(price).length();
        StringBuilder builder = new StringBuilder();
        builder.append('\u00A4');

        if (fractionDigits == 0) {
            for (int i = 0; i < totalLength; i++) {
                builder.append('#');
            }
            DecimalFormat noDecimalCurrencyFormat = new DecimalFormat(builder.toString());
            noDecimalCurrencyFormat.setCurrency(displayCurrency);
            return noDecimalCurrencyFormat.format(price);
        }

        int beforeDecimal = totalLength - fractionDigits;
        for (int i = 0; i < beforeDecimal; i++) {
            builder.append('#');
        }
        // So we display "$0.55" instead of "$.55"
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
        decimalFormat.setCurrency(displayCurrency);

        return decimalFormat.format(decimalPrice);
    }
}

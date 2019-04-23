package com.stripe.android.view;

import android.support.annotation.NonNull;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class PaymentUtils {

    /**
     * Formats a monetary amount into a human friendly string where zero is returned
     * as free.
     */
    static String formatPriceStringUsingFree(long amount, @NonNull Currency currency, String free) {
        if (amount == 0) {
            return free;
        }
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat)
                .getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(currency.getSymbol(Locale.getDefault()));
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return formatPriceString(amount, currency);
    }

    /**
     * Formats a monetary amount into a human friendly string.
     */
    static String formatPriceString(double amount, @NonNull Currency currency) {
        double majorUnitAmount = amount / Math.pow(10, currency.getDefaultFractionDigits());
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        try {
            DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat)
                    .getDecimalFormatSymbols();
            decimalFormatSymbols.setCurrencySymbol(currency.getSymbol(Locale.getDefault()));
            ((java.text.DecimalFormat) currencyFormat)
                    .setDecimalFormatSymbols(decimalFormatSymbols);
            return currencyFormat.format(majorUnitAmount);
        } catch (ClassCastException e) {
            return currencyFormat.format(majorUnitAmount);
        }
    }

}

package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;

import com.stripe.android.R;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class PaymentUtils {

    private static double ZERO_CENTS_EPSILON = 0.001;

    /**
     * Formats a monetary amount into a human friendly string where zero(-ish) is returned
     * as free.
     */
    static String formatPriceStringUsingFree(@NonNull Context context, double amount, @NonNull Currency currency) {
        if (-ZERO_CENTS_EPSILON < amount && amount < ZERO_CENTS_EPSILON) {
            return context.getResources().getString(R.string.price_free);
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(currency.getSymbol(Locale.getDefault()));
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return formatPriceString(amount, currency);
    }

    /**
     * Formats a monetary amount into a human friendly string.
     */
    static String formatPriceString(double amount, @NonNull Currency currency) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(currency.getSymbol(Locale.getDefault()));
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);
        return currencyFormat.format(amount);
    }

}

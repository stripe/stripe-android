package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;

import com.stripe.android.R;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class PaymentUtils {

    /**
     * Formats an monetary amount into a human friendly string.
     */
    static String getPriceString(@NonNull Context context, double amount, @NonNull Currency currency) {
        if (amount == 0) {
            return context.getResources().getString(R.string.shipping_free);
        }
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(currency.getSymbol(Locale.getDefault()));
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return currencyFormat.format(amount);
    }

}

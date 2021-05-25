package com.stripe.android.view

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

object PaymentUtils {

    /**
     * Formats a monetary amount into a human friendly string where zero is returned
     * as free.
     */
    @JvmStatic
    fun formatPriceStringUsingFree(
        amount: Long,
        currency: Currency,
        free: String
    ): String {
        if (amount == 0L) {
            return free
        }

        val currencyFormat = NumberFormat.getCurrencyInstance()
        val decimalFormatSymbols = (currencyFormat as DecimalFormat)
            .decimalFormatSymbols
        decimalFormatSymbols.currencySymbol = currency.getSymbol(Locale.getDefault())
        currencyFormat.decimalFormatSymbols = decimalFormatSymbols

        return formatPriceString(amount.toDouble(), currency)
    }

    /**
     * Formats a monetary amount into a human friendly string.
     */
    @JvmSynthetic
    internal fun formatPriceString(amount: Double, currency: Currency): String {
        val majorUnitAmount = amount / 10.0.pow(currency.defaultFractionDigits.toDouble())
        val currencyFormat = NumberFormat.getCurrencyInstance()
        return try {
            val decimalFormatSymbols = (currencyFormat as DecimalFormat)
                .decimalFormatSymbols
            decimalFormatSymbols.currencySymbol = currency.getSymbol(Locale.getDefault())
            currencyFormat.decimalFormatSymbols = decimalFormatSymbols
            currencyFormat.format(majorUnitAmount)
        } catch (e: ClassCastException) {
            currencyFormat.format(majorUnitAmount)
        }
    }
}

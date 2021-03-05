package com.stripe.android.paymentsheet

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

internal class CurrencyFormatter {
    fun format(
        amount: Long,
        currency: Currency
    ): String {
        val currencySymbol = currency.getSymbol(Locale.getDefault())
        val majorUnitAmount =
            amount / MAJOR_UNIT_BASE.pow(getDefaultFractionDigits(currency).toDouble())
        val currencyFormat = NumberFormat.getCurrencyInstance()

        // We need to cast inside the try catch because most currencies are decimal formats but
        // not all. See the official Google Docs for NumberFormat for more context.
        runCatching {
            val decimalFormatSymbols =
                (currencyFormat as DecimalFormat).decimalFormatSymbols
            decimalFormatSymbols.currency = currency
            decimalFormatSymbols.currencySymbol = currencySymbol
            currencyFormat.decimalFormatSymbols = decimalFormatSymbols
        }

        return currencyFormat.format(majorUnitAmount)
    }

    private fun getDefaultFractionDigits(currency: Currency): Int {
        /**
         * Handle special cases where the client's default fractional digits for a given currency
         * don't match the Stripe backend's assumption.
         */
        return when (currency.currencyCode.toUpperCase(Locale.ROOT)) {
            "AFN" -> 2 // Afghanistan afghani
            "ALL" -> 2 // Albanian lek
            "AMD" -> 2 // Armenian dram
            "COP" -> 2 // Colombia peso
            "IDR" -> 2 // Indonesian rupiah
            "ISK" -> 2 // Icelandic króna
            "PKR" -> 2 // Pakistani rupee
            "LBP" -> 2 // Lebanese pound (a.k.a. lira)
            else -> currency.defaultFractionDigits
        }
    }

    private companion object {
        private const val MAJOR_UNIT_BASE = 10.0
    }
}

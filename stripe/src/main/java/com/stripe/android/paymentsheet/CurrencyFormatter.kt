package com.stripe.android.paymentsheet

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

internal class CurrencyFormatter {

    fun format(
        amount: Long,
        amountCurrencyCode: String,
        targetLocale: Locale = Locale.getDefault()
    ) = format(
        amount,
        Currency.getInstance(
            amountCurrencyCode.toUpperCase(Locale.ROOT)
        ),
        targetLocale
    )

    fun format(
        amount: Long,
        amountCurrency: Currency,
        targetLocale: Locale = Locale.getDefault()
    ): String {
        val defaultCurrencyDigits = getDefaultFractionDigits(amountCurrency)
        val majorUnitAmount =
            amount / MAJOR_UNIT_BASE.pow(defaultCurrencyDigits.toDouble())

        /**
         * The currencyFormat for a country and region specifies many things including:
         * - do they use decimal places (some currencies like Korea's is whole numbers only)
         * - what is the symbol for the currency
         * - where does the symbol go (right or left of the number)
         * - how do they format decimal separators (i.e. France uses commas)
         * - how do they separate thousand digits (i.e. France uses spaces)
         * When you get the currencyInstance you are getting the symbol for the target currency
         * (i.e. francs) where they place their symbol for currency, etc.
         *
         * Some fields not used here, but might be relevant in other scenarios:
         * - positive and negative numbers
         *
         * However, the currency of the amount is different, the amount might have decimal places
         * even if the target currency does not, and we want to use the symbol for the currency
         * amount, and not the currency of the target.  So below we will switch these out.
         */
        val currencyFormat = NumberFormat.getCurrencyInstance(targetLocale)

        // We need to cast inside the try catch because most currencies are decimal formats but
        // not all. See the official Google Docs for NumberFormat for more context.
        runCatching {
            val decimalFormatSymbols =
                (currencyFormat as DecimalFormat).decimalFormatSymbols
            decimalFormatSymbols.currency = amountCurrency
            decimalFormatSymbols.currencySymbol = amountCurrency.getSymbol(targetLocale)
            currencyFormat.minimumFractionDigits = defaultCurrencyDigits
            currencyFormat.decimalFormatSymbols = decimalFormatSymbols
        }

        return currencyFormat.format(majorUnitAmount)
    }

    private fun getDefaultFractionDigits(currency: Currency): Int {
        /**
         * Handle special cases where the client's default fractional digits for a given currency
         * don't match the Stripe backend's assumption.
         */
        return if (SERVER_DECIMAL_DIGITS_2.contains(currency.currencyCode.toUpperCase(Locale.ROOT))) {
            2
        } else {
            currency.defaultFractionDigits
        }
    }

    internal companion object {
        private const val MAJOR_UNIT_BASE = 10.0
        internal val SERVER_DECIMAL_DIGITS_2 =
            setOf("UGX", "AFN", "ALL", "AMD", "COP", "IDR", "ISK", "PKR", "LBP")
    }
}

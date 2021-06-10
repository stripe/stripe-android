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
        Currency.getInstance(amountCurrencyCode.uppercase()),
        targetLocale
    )

    fun format(
        amount: Long,
        amountCurrency: Currency,
        targetLocale: Locale = Locale.getDefault()
    ): String {
        val amountCurrencyDecimalDigits = getDefaultDecimalDigits(amountCurrency)
        val majorUnitAmount =
            amount / MAJOR_UNIT_BASE.pow(amountCurrencyDecimalDigits.toDouble())

        /**
         * The currencyFormat for a country and region specifies many things including:
         * - do they use decimal places (e.g. Korea's is whole numbers only)
         * - what is the symbol for the currency
         * - where does the symbol go (right or left of the number)
         * - how do they format decimal separators (e.g. France uses commas)
         * - how do they separate thousand digits (e.g. France uses spaces)
         * When you get the [NumberFormat#getCurrencyInstance] you are getting all this information.
         *
         * Some fields not used here, but might be relevant in other scenarios:
         * - positive and negative numbers
         *
         * Among other things the currency format of the amount might have decimal places
         * even if the targetLocale's currency does not, so below we will do a sort of merge of
         * the targetLocale and amount currency. We will start with the currency format of the
         * targetLocale, this sets, most notably, the number decimal and thousands separators,
         * where the currency symbol should be placed relative to the number. Then we find the
         * currency symbol of the amount for the targetLocale, and use that. Finally, we set the
         * number of decimal places used by the amount currency.
         *
         * See the [NumberFormat] for why we use a try block.
         */
        val currencyFormat = NumberFormat.getCurrencyInstance(targetLocale)

        runCatching {
            val decimalFormatSymbols =
                (currencyFormat as DecimalFormat).decimalFormatSymbols
            decimalFormatSymbols.currency = amountCurrency
            decimalFormatSymbols.currencySymbol = amountCurrency.getSymbol(targetLocale)
            currencyFormat.minimumFractionDigits = amountCurrencyDecimalDigits
            currencyFormat.decimalFormatSymbols = decimalFormatSymbols
        }

        return currencyFormat.format(majorUnitAmount)
    }

    private fun getDefaultDecimalDigits(currency: Currency): Int {
        /**
         * Handle special cases where the client's default fractional digits for a given currency
         * don't match the Stripe backend's assumption.
         */
        return SERVER_DECIMAL_DIGITS
            .filter { entry ->
                entry.key.contains(currency.currencyCode.uppercase())
            }.map {
                it.value
            }.firstOrNull() ?: currency.defaultFractionDigits
    }

    internal companion object {
        private const val MAJOR_UNIT_BASE = 10.0
        private val SERVER_DECIMAL_DIGITS =
            mapOf(
                setOf("UGX", "AFN", "ALL", "AMD", "COP", "IDR", "ISK", "PKR", "LBP") to 2
            )
    }
}

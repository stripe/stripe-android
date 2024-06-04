package com.stripe.android

import com.stripe.android.uicore.format.CurrencyFormatter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

/**
 * Public utility class for common Pay with Google-related tasks.
 */
object PayWithGoogleUtils {

    /**
     * Converts an integer price in the lowest currency denomination to a Google string value.
     * For instance: (100L, USD) -> "1.00", but (100L, JPY) -> "100".
     * @param price the price in the lowest available currency denomination
     * @param currency the [Currency] used to determine how many digits after the decimal
     * @return a String that can be used as a Pay with Google price string
     */
    @Deprecated(
        message = "Use getPriceString(Long, Currency) instead.",
        replaceWith = ReplaceWith("getPriceString(price.toLong(), currency)"),
    )
    @JvmStatic
    fun getPriceString(price: Int, currency: Currency): String {
        return getPriceString(price.toLong(), currency)
    }

    /**
     * Converts an integer price in the lowest currency denomination to a Google string value.
     * For instance: (100L, USD) -> "1.00", but (100L, JPY) -> "100".
     * @param price the price in the lowest available currency denomination
     * @param currency the [Currency] used to determine how many digits after the decimal
     * @return a String that can be used as a Pay with Google price string
     */
    @JvmStatic
    fun getPriceString(price: Long, currency: Currency): String {
        val fractionDigits = CurrencyFormatter.getDefaultDecimalDigits(currency)
        val totalLength = price.toString().length
        val builder = StringBuilder()

        if (fractionDigits == 0) {
            for (i in 0 until totalLength) {
                builder.append('#')
            }
            val noDecimalCurrencyFormat =
                DecimalFormat(builder.toString(), DecimalFormatSymbols.getInstance(Locale.ROOT))
            noDecimalCurrencyFormat.currency = currency
            noDecimalCurrencyFormat.isGroupingUsed = false
            return noDecimalCurrencyFormat.format(price)
        }

        val beforeDecimal = totalLength - fractionDigits
        for (i in 0 until beforeDecimal) {
            builder.append('#')
        }

        // So we display "0.55" instead of ".55"
        if (totalLength <= fractionDigits) {
            builder.append('0')
        }
        builder.append('.')
        for (i in 0 until fractionDigits) {
            builder.append('0')
        }
        val modBreak = 10.0.pow(fractionDigits.toDouble())
        val decimalPrice = price / modBreak

        // No matter the Locale, Android Pay requires a dot for the decimal separator, and Arabic
        // numbers.
        val symbolOverride = DecimalFormatSymbols.getInstance(Locale.ROOT)
        val decimalFormat = DecimalFormat(builder.toString(), symbolOverride)
        decimalFormat.currency = currency
        decimalFormat.isGroupingUsed = false

        return decimalFormat.format(decimalPrice)
    }
}

package com.stripe.android.uicore.format

import android.icu.text.NumberFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Currency
import java.util.Locale

/**
 * See [currencies](https://stripe.com/docs/currencies#minimum-and-maximum-charge-amounts)
 * for useful test scenarios.
 *
 * Also by looking at the [NumberFormat] fields returned from [NumberFormat#getCurrencyInstance]
 * we see other things that vary between countries. Each one is not verified here as we are not testing
 * the library.
 *
 * The focus of this testing is to make sure the:
 *      - amount is formatted with the number of decimal digits based on the amount currency, and
 *      not the locale currency
 *      - decimal number is formatted in the locale format
 *      - currency symbol location is formatted in the locale format
 *      - currency symbol is formatted in the locale format
 *
 * If we added instrumented tests to our build server, this would be a good set of cases to run on device.
 *
 * Notable in testing is that if the targetLocale has a language vs a language and
 * country the outputted results are quite different.
 *
 * Similar tests exist on iOS
 * [here](https://github.com/stripe/stripe-ios/blob/master/Tests/Tests/NSString+StripeTest.swift)
 */
class CurrencyFormatterTest {
    private fun normalizeSpaces(s: String): String {
        return s
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun assertFormattedEquals(
        actual: String,
        expected: String,
    ) {
        assertThat(normalizeSpaces(actual)).isEqualTo(normalizeSpaces(expected))
    }

    private fun assertFormattedEquals(
        amount: Long,
        amountCurrency: Currency,
        targetLocale: Locale = Locale.getDefault(),
        expected: String,
    ) {
        assertFormattedEquals(CurrencyFormatter.format(amount, amountCurrency, targetLocale), expected)
    }

    @Test
    fun `amount currency USD, locale US`() {
        assertFormattedEquals(123412L, Currency.getInstance("USD"), Locale.US, "$1,234.12")
    }

    @Test
    fun `amount currency Germany, locale US`() {
        val euro = Currency.getInstance(Locale.GERMANY)
        assertFormattedEquals(123412L, euro, Locale.US, "€1,234.12")
    }

    @Test
    fun `amount currency Canada, locale US`() {
        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertFormattedEquals(123412L, canadianDollar, Locale.US, "CA$1,234.12")
    }

    @Test
    fun `amount currency UK, locale US`() {
        val britishPound = Currency.getInstance(Locale.UK)
        assertFormattedEquals(123412L, britishPound, Locale.US, "£1,234.12")
    }

    @Test
    fun `amount currency 2-decimal, and locale currency of 0 decimal`() {
        assertFormattedEquals(123412L, Currency.getInstance("USD"), Locale.KOREA, "US$1,234.12")
    }

    @Test
    fun `amount currency 0-decimal, and locale currency of 2 decimal`() {
        assertFormattedEquals(1234L, Currency.getInstance("KRW"), Locale.US, "₩1,234")
    }

    @Test
    fun `find currencies with non-2 decimal digits`() {
        // This is interesting to look to compare to the server-side
        Currency.getAvailableCurrencies()
            .filter { it.defaultFractionDigits != 2 }
            .sortedBy {
                it.currencyCode
            }
            .forEach {
                print(String.format(Locale.getDefault(), "%s, ", it.currencyCode))
            }
    }

    @Test
    fun `HUF is effectively 0 decimal places, but Stripe treats it as 2`() {
        val amountCurrency = Currency.getInstance("HUF")
        assertThat(CurrencyFormatter.getDefaultDecimalDigits(amountCurrency)).isEqualTo(2)
        assertFormattedEquals(123412L, amountCurrency, LOCALE_WITH_3_DECIMAL_CURRENCY, "HUF 1,234.12")
    }

    @Test
    fun `UGX requires 2 decimal for backward compatibility of the currency`() {
        val amountCurrency = Currency.getInstance("UGX")
        assertFormattedEquals(123412L, amountCurrency, LOCALE_WITH_3_DECIMAL_CURRENCY, "UGX 1,234.12")
    }

    @Test
    fun `Amount currency with 3 decimal places from a locale that normally has 2`() {
        val amountCurrency = Currency.getInstance("BHD")
        assertFormattedEquals(1234123L, amountCurrency, Locale.US, "BHD1,234.123")
    }

    @Test
    fun `Amount currency with 2 decimal places from a locale that normally has 3`() {
        val amountCurrency = Currency.getInstance("USD")
        assertFormattedEquals(123412L, amountCurrency, LOCALE_WITH_3_DECIMAL_CURRENCY, "US$ 1,234.12")
    }

    @Test
    fun `locale with unique thousands and decimal separator, currency symbol at the end`() {
        assertFormattedEquals(123412L, Currency.getInstance("USD"), Locale.FRANCE, "1 234,12 \$US")
    }

    @Test
    fun `amount currency not-zero-based, with locale zero-based currency`() {
        assertFormattedEquals(123412L, Currency.getInstance("USD"), LOCALE_ICELAND_LANGUAGE_ONLY, "1.234,12 USD")
    }

    @Test
    fun `Test differences in setting the language vs country vs both`() {
        assertFormattedEquals(123412L, Currency.getInstance("USD"), Locale("IS"), "1.234,12 USD")

        assertFormattedEquals(123412L, Currency.getInstance("USD"), Locale("is-IS", "IS"), "US\$ 1,234.12")
    }

    @Test
    fun `test UK with thousands of dollars`() {
        assertFormattedEquals(123412L, Currency.getInstance("USD"), Locale.UK, "US$1,234.12")
    }

    @Test
    fun `test AU with thousands of dollars`() {
        assertFormattedEquals(123412L, Currency.getInstance("USD"), LOCALE_AUSTRALIA_LANGUAGE_COUNTRY, "US$ 1,234.12")

        assertFormattedEquals(123412L, Currency.getInstance("USD"), Locale("AU"), "US$ 1,234.12")
    }

    @Test
    fun `Treats MMK as a two-decimal currency`() {
        val currency = Currency.getInstance("MMK")
        val formattedAmount = CurrencyFormatter.format(5099L, currency)
        assertThat(normalizeSpaces(formattedAmount)).isEqualTo(normalizeSpaces("MMK50.99"))
    }

    @Test
    fun `Treats LAK as a two-decimal currency`() {
        val currency = Currency.getInstance("LAK")
        val formattedAmount = CurrencyFormatter.format(5099L, currency)
        assertThat(normalizeSpaces(formattedAmount)).isEqualTo(normalizeSpaces("LAK50.99"))
    }

    @Test
    fun `Treats RSD as a two-decimal currency`() {
        val currency = Currency.getInstance("RSD")
        val formattedAmount = CurrencyFormatter.format(5099L, currency)
        assertThat(normalizeSpaces(formattedAmount)).isEqualTo(normalizeSpaces("RSD50.99"))
    }

    companion object {
        val LOCALE_ICELAND_LANGUAGE_ONLY = Locale("IS")
        val LOCALE_AUSTRALIA_LANGUAGE_COUNTRY = Locale("en-AU", "AU")
        val LOCALE_WITH_3_DECIMAL_CURRENCY = Locale("ar-BH", "BH")
    }
}

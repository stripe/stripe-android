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
    @Test
    fun `amount currency USD, locale US`() {
        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                Locale.US,
            )
        ).isEqualTo("$1,234.12")
    }

    @Test
    fun `amount currency Germany, locale US`() {
        val euro = Currency.getInstance(Locale.GERMANY)
        assertThat(CurrencyFormatter.format(123412L, euro, Locale.US))
            .isEqualTo("€1,234.12")
    }

    @Test
    fun `amount currency Canada, locale US`() {
        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertThat(CurrencyFormatter.format(123412L, canadianDollar, Locale.US))
            .isEqualTo("CA$1,234.12")
    }

    @Test
    fun `amount currency UK, locale US`() {
        val britishPound = Currency.getInstance(Locale.UK)
        assertThat(CurrencyFormatter.format(123412L, britishPound, Locale.US))
            .isEqualTo("£1,234.12")
    }

    @Test
    fun `amount currency 2-decimal, and locale currency of 0 decimal`() {
        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                Locale.KOREA
            )
        )
            .isEqualTo("US$1,234.12")
    }

    @Test
    fun `amount currency 0-decimal, and locale currency of 2 decimal`() {
        assertThat(
            CurrencyFormatter.format(
                1234L, // this currency does not have decimals
                Currency.getInstance("KRW"),
                Locale.US
            )
        )
            .isEqualTo("₩1,234")
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
        assertThat(
            CurrencyFormatter.format(
                123412L,
                amountCurrency,
                LOCALE_WITH_3_DECIMAL_CURRENCY
            )
        )
            .isEqualTo("HUF 1,234.12")
    }

    @Test
    fun `UGX requires 2 decimal for backward compatibility of the currency`() {
        val amountCurrency = Currency.getInstance("UGX")
        assertThat(
            CurrencyFormatter.format(
                123412L,
                amountCurrency,
                LOCALE_WITH_3_DECIMAL_CURRENCY
            )
        )
            .isEqualTo("UGX 1,234.12")
    }

    @Test
    fun `Amount currency with 3 decimal places from a locale that normally has 2`() {
        val amountCurrency = Currency.getInstance("BHD")
        assertThat(CurrencyFormatter.format(1234123L, amountCurrency, Locale.US))
            .isEqualTo("BHD1,234.123")
    }

    @Test
    fun `Amount currency with 2 decimal places from a locale that normally has 3`() {
        val amountCurrency = Currency.getInstance("USD")
        assertThat(
            CurrencyFormatter.format(
                123412L,
                amountCurrency,
                LOCALE_WITH_3_DECIMAL_CURRENCY
            )
        )
            .isEqualTo("US$ 1,234.12")
    }

    @Test
    fun `locale with unique thousands and decimal separator, currency symbol at the end`() {
        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                Locale.FRANCE
            )
        )
            .isEqualTo("1 234,12 \$US")
    }

    @Test
    fun `amount currency not-zero-based, with locale zero-based currency`() {
        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                LOCALE_ICELAND_LANGUAGE_ONLY
            )
        )
            .isEqualTo("1.234,12 USD")
    }

    @Test
    fun `Test differences in setting the language vs country vs both`() {
        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                Locale("IS")
            )
        )
            .isEqualTo("1.234,12 USD")

        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                Locale("is-IS", "IS")
            )
        )
            .isEqualTo("US$ 1,234.12")
    }

    @Test
    fun `test UK with thousands of dollars`() {
        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                Locale.UK
            )
        )
            .isEqualTo("US$1,234.12")
    }

    @Test
    fun `test AU with thousands of dollars`() {
        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                LOCALE_AUSTRALIA_LANGUAGE_COUNTRY
            )
        )
            .isEqualTo("US$ 1,234.12")

        assertThat(
            CurrencyFormatter.format(
                123412L,
                Currency.getInstance("USD"),
                Locale("AU")
            )
        )
            .isEqualTo("US$ 1,234.12")
    }

    @Test
    fun `Treats MMK as a two-decimal currency`() {
        val currency = Currency.getInstance("MMK")
        val formattedAmount = CurrencyFormatter.format(5099L, currency)
        assertThat(formattedAmount).isEqualTo("MMK50.99")
    }

    @Test
    fun `Treats LAK as a two-decimal currency`() {
        val currency = Currency.getInstance("LAK")
        val formattedAmount = CurrencyFormatter.format(5099L, currency)
        assertThat(formattedAmount).isEqualTo("LAK50.99")
    }

    @Test
    fun `Treats RSD as a two-decimal currency`() {
        val currency = Currency.getInstance("RSD")
        val formattedAmount = CurrencyFormatter.format(5099L, currency)
        assertThat(formattedAmount).isEqualTo("RSD50.99")
    }

    companion object {
        val LOCALE_ICELAND_LANGUAGE_ONLY = Locale("IS")
        val LOCALE_AUSTRALIA_LANGUAGE_COUNTRY = Locale("en-AU", "AU")
        val LOCALE_WITH_3_DECIMAL_CURRENCY = Locale("ar-BH", "BH")
    }
}

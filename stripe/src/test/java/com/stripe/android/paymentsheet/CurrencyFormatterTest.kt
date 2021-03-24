package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Currency
import java.util.Locale

class CurrencyFormatterTest {
    private val currencyFormatter = CurrencyFormatter()

    @Test
    fun `format returns expected value`() {
        Locale.setDefault(Locale.US)
        assertThat(
            currencyFormatter.format(
                12355L,
                Currency.getInstance("USD")
            )
        ).isEqualTo("$123.55")

        val euro = Currency.getInstance(Locale.GERMANY)
        assertThat(currencyFormatter.format(12300L, euro))
            .isEqualTo("€123.00")

        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertThat(currencyFormatter.format(12300L, canadianDollar))
            .isEqualTo("CA$123.00")

        val britishPound = Currency.getInstance(Locale.UK)
        assertThat(currencyFormatter.format(10000L, britishPound))
            .isEqualTo("£100.00")
    }

    @Test
    fun `test korea with us denomination`() {
        assertThat(
            currencyFormatter.format(
                50,
                Currency.getInstance("USD"),
                Locale.KOREA
            )
        )
            .isEqualTo("US$0.50")
    }

    @Test
    fun `test france with thousands of dollars`() {
        assertThat(
            currencyFormatter.format(
                100000,
                Currency.getInstance("USD"),
                Locale.FRANCE
            )
        )
            .isEqualTo("US$ 1,000.00")
    }
}

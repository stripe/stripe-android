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
            .isEqualTo("EUR123.00")

        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertThat(currencyFormatter.format(12300L, canadianDollar))
            .isEqualTo("CAD123.00")

        val britishPound = Currency.getInstance(Locale.UK)
        assertThat(currencyFormatter.format(10000L, britishPound))
            .isEqualTo("GBP100.00")
    }
}

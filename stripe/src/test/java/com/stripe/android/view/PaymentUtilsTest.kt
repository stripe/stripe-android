package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import java.util.Currency
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [PaymentUtils]
 */
@RunWith(RobolectricTestRunner::class)
class PaymentUtilsTest {
    @BeforeTest
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun formatPriceStringUsingFree_whenZero_rendersFree() {
        assertThat(
            PaymentUtils.formatPriceStringUsingFree(
                0,
                USD,
                "Free"
            )
        ).isEqualTo("Free")
    }

    @Test
    fun formatPriceString_whenUSLocale_rendersCorrectSymbols() {
        Locale.setDefault(Locale.US)
        assertThat(PaymentUtils.formatPriceString(12300.0, USD))
            .isEqualTo("$123.00")

        assertThat(PaymentUtils.formatPriceString(12300.0, EURO))
            .isEqualTo("EUR123.00")

        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertThat(PaymentUtils.formatPriceString(12300.0, canadianDollar))
            .isEqualTo("CAD123.00")

        val britishPound = Currency.getInstance(Locale.UK)
        assertThat(PaymentUtils.formatPriceString(10000.0, britishPound))
            .isEqualTo("GBP100.00")
    }

    @Test
    fun formatPriceString_whenInternationalLocale_rendersCorrectSymbols() {
        Locale.setDefault(Locale.GERMANY)
        assertThat(PaymentUtils.formatPriceString(10000.0, EURO))
            .isEqualTo("100,00 €")

        Locale.setDefault(Locale.JAPAN)
        val yen = Currency.getInstance(Locale.JAPAN)
        // Japan's native local uses narrow yen symbol (there is also a wide yen symbol)
        assertThat(PaymentUtils.formatPriceString(100.0, yen))
            .isEqualTo("￥100")

        Locale.setDefault(Locale("ar", "JO")) // Jordan
        val jordanianDinar = Currency.getInstance("JOD")
        assertThat(PaymentUtils.formatPriceString(100123.0, jordanianDinar))
            .isEqualTo("${jordanianDinar.symbol} 100.123")

        Locale.setDefault(Locale.UK)
        val britishPound = Currency.getInstance(Locale.UK)
        assertThat(PaymentUtils.formatPriceString(10000.0, britishPound))
            .isEqualTo("£100.00")

        Locale.setDefault(Locale.CANADA)
        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertThat(PaymentUtils.formatPriceString(12300.0, canadianDollar))
            .isEqualTo("$123.00")
    }

    @Test
    fun formatPriceString_whenDecimalAmounts_rendersCorrectDigits() {
        assertThat(PaymentUtils.formatPriceString(10012.0, USD))
            .isEqualTo("$100.12")
        assertThat(PaymentUtils.formatPriceString(12.0, USD))
            .isEqualTo("$0.12")
    }

    private companion object {
        private val USD = Currency.getInstance("USD")
        private val EURO = Currency.getInstance(Locale.GERMANY)
    }
}

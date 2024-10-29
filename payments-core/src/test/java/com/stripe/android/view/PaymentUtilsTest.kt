package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Currency
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

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
                Currency.getInstance("USD"),
                "Free"
            )
        ).isEqualTo("Free")
    }

    @Test
    fun formatPriceString_whenUSLocale_rendersCorrectSymbols() {
        Locale.setDefault(Locale.US)
        assertThat(
            PaymentUtils.formatPriceString(
                12300.0,
                Currency.getInstance("USD")
            )
        ).isEqualTo("$123.00")

        val euro = Currency.getInstance(Locale.GERMANY)
        assertThat(PaymentUtils.formatPriceString(12300.0, euro))
            .isEqualTo("€123.00")

        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertThat(PaymentUtils.formatPriceString(12300.0, canadianDollar))
            .isEqualTo("CA$123.00")

        val britishPound = Currency.getInstance(Locale.UK)
        assertThat(PaymentUtils.formatPriceString(10000.0, britishPound))
            .isEqualTo("£100.00")
    }

    @Test
    fun formatPriceString_whenInternationalLocale_rendersCorrectSymbols() {
        Locale.setDefault(Locale.GERMANY)
        val euro = Currency.getInstance(Locale.GERMANY)
        assertThat(PaymentUtils.formatPriceString(10000.0, euro))
            .isEqualTo("100,00 €")

        Locale.setDefault(Locale.JAPAN)
        val yen = Currency.getInstance(Locale.JAPAN)
        // Japan's native local uses narrow yen symbol (there is also a wide yen symbol)
        assertThat(PaymentUtils.formatPriceString(100.0, yen))
            .isEqualTo("￥100")

        Locale.setDefault(Locale("ar", "JO")) // Jordan
        val jordanianDinar = Currency.getInstance("JOD")
        assertThat(PaymentUtils.formatPriceString(100123.0, jordanianDinar))
            .isEqualTo("\u200F١٠٠٫١٢٣ د.أ.\u200F")

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
        assertThat(
            PaymentUtils.formatPriceString(
                10012.0,
                Currency.getInstance("USD")
            )
        ).isEqualTo("$100.12")
        assertThat(
            PaymentUtils.formatPriceString(
                12.0,
                Currency.getInstance("USD")
            )
        ).isEqualTo("$0.12")
    }
}

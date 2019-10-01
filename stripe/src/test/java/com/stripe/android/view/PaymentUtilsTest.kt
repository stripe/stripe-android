package com.stripe.android.view

import java.util.Currency
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals(
            PaymentUtils.formatPriceStringUsingFree(
                0,
                Currency.getInstance("USD"),
                "Free"),
            "Free")
    }

    @Test
    fun formatPriceString_whenUSLocale_rendersCorrectSymbols() {
        Locale.setDefault(Locale.US)
        assertEquals(
            PaymentUtils.formatPriceString(12300.0, Currency.getInstance("USD")),
            "$123.00"
        )

        val euro = Currency.getInstance(Locale.GERMANY)
        assertEquals(PaymentUtils.formatPriceString(12300.0, euro), "EUR123.00")

        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertEquals(
            PaymentUtils.formatPriceString(12300.0, canadianDollar),
            "CAD123.00"
        )

        val britishPound = Currency.getInstance(Locale.UK)
        assertEquals(PaymentUtils.formatPriceString(10000.0, britishPound), "GBP100.00")
    }

    @Test
    fun formatPriceString_whenInternationalLocale_rendersCorrectSymbols() {
        Locale.setDefault(Locale.GERMANY)
        val euro = Currency.getInstance(Locale.GERMANY)
        assertEquals(PaymentUtils.formatPriceString(10000.0, euro), "100,00 €")

        Locale.setDefault(Locale.JAPAN)
        val yen = Currency.getInstance(Locale.JAPAN)
        // Japan's native local uses narrow yen symbol (there is also a wide yen symbol)
        assertEquals(PaymentUtils.formatPriceString(100.0, yen), "￥100")

        Locale.setDefault(Locale("ar", "JO")) // Jordan
        val jordanianDinar = Currency.getInstance("JOD")
        assertEquals(PaymentUtils.formatPriceString(100123.0, jordanianDinar), jordanianDinar.symbol + " 100.123")

        Locale.setDefault(Locale.UK)
        val britishPound = Currency.getInstance(Locale.UK)
        assertEquals(PaymentUtils.formatPriceString(10000.0, britishPound), "£100.00")

        Locale.setDefault(Locale.CANADA)
        val canadianDollar = Currency.getInstance(Locale.CANADA)
        assertEquals(
            PaymentUtils.formatPriceString(12300.0, canadianDollar),
            "$123.00"
        )
    }

    @Test
    fun formatPriceString_whenDecimalAmounts_rendersCorrectDigits() {
        assertEquals(
            "$100.12",
            PaymentUtils.formatPriceString(10012.0, Currency.getInstance("USD"))
        )
        assertEquals(
            "$0.12",
            PaymentUtils.formatPriceString(12.0, Currency.getInstance("USD"))
        )
    }
}

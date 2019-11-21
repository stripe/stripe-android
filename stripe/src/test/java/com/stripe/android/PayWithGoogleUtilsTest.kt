package com.stripe.android

import com.stripe.android.PayWithGoogleUtils.getPriceString
import java.util.Currency
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for [PayWithGoogleUtils].
 */
class PayWithGoogleUtilsTest {

    @AfterTest
    fun tearDown() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun getPriceString_whenCurrencyWithDecimals_returnsExpectedValue() {
        val priceString = getPriceString(100, Currency.getInstance("USD"))
        assertEquals("1.00", priceString)

        val littlePrice = getPriceString(8, Currency.getInstance("EUR"))
        assertEquals("0.08", littlePrice)

        val bigPrice = getPriceString(20000000, Currency.getInstance("GBP"))
        assertEquals("200000.00", bigPrice)
    }

    @Test
    fun getPriceString_whenLocaleWithCommas_returnsExpectedValue() {
        Locale.setDefault(Locale.FRENCH)
        val priceString = getPriceString(100, Currency.getInstance("USD"))
        assertEquals("1.00", priceString)

        val littlePrice = getPriceString(8, Currency.getInstance("EUR"))
        assertEquals("0.08", littlePrice)

        val bigPrice = getPriceString(20000000, Currency.getInstance("GBP"))
        assertEquals("200000.00", bigPrice)
    }

    @Test
    fun getPriceString_whenCurrencyWithoutDecimals_returnsExpectedValue() {
        val priceString = getPriceString(250, Currency.getInstance("JPY"))
        assertEquals("250", priceString)

        val bigPrice = getPriceString(250000, Currency.getInstance("KRW"))
        assertEquals("250000", bigPrice)

        val littlePrice = getPriceString(7, Currency.getInstance("CLP"))
        assertEquals("7", littlePrice)
    }
}

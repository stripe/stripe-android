package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PayWithGoogleUtils.getPriceString
import java.util.Currency
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test

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
        val priceString = getPriceString(100L, Currency.getInstance("USD"))
        assertThat(priceString).isEqualTo("1.00")

        val littlePrice = getPriceString(8L, Currency.getInstance("EUR"))
        assertThat(littlePrice).isEqualTo("0.08")

        val bigPrice = getPriceString(20000000L, Currency.getInstance("GBP"))
        assertThat(bigPrice).isEqualTo("200000.00")
    }

    @Test
    fun getPriceString_whenLocaleWithCommas_returnsExpectedValue() {
        Locale.setDefault(Locale.FRENCH)
        val priceString = getPriceString(100L, Currency.getInstance("USD"))
        assertThat(priceString).isEqualTo("1.00")

        val littlePrice = getPriceString(8L, Currency.getInstance("EUR"))
        assertThat(littlePrice).isEqualTo("0.08")

        val bigPrice = getPriceString(20000000L, Currency.getInstance("GBP"))
        assertThat(bigPrice).isEqualTo("200000.00")
    }

    @Test
    fun getPriceString_whenCurrencyWithoutDecimals_returnsExpectedValue() {
        val priceString = getPriceString(250L, Currency.getInstance("JPY"))
        assertThat(priceString).isEqualTo("250")

        val bigPrice = getPriceString(250000L, Currency.getInstance("KRW"))
        assertThat(bigPrice).isEqualTo("250000")

        val littlePrice = getPriceString(7L, Currency.getInstance("CLP"))
        assertThat(littlePrice).isEqualTo("7")
    }

    @Test
    fun getPriceString_whenLocaleWithArabicNumerals_returnsExpectedValue() {
        Locale.setDefault(Locale.Builder().setLanguage("ar").setRegion("AE").build())
        val priceString = getPriceString(100L, Currency.getInstance("USD"))
        assertThat(priceString).isEqualTo("1.00")

        val littlePrice = getPriceString(8L, Currency.getInstance("EUR"))
        assertThat(littlePrice).isEqualTo("0.08")

        val bigPrice = getPriceString(20000000L, Currency.getInstance("GBP"))
        assertThat(bigPrice).isEqualTo("200000.00")
    }

    @Test
    fun getPriceString_whenLocaleWithWrongSystemDecimalDigits_returnsExpectedValue() {
        // Currency.defaultFractionDigits returns an incorrect value for LAK
        val priceString = getPriceString(2_000_000L, Currency.getInstance("LAK"))
        assertThat(priceString).isEqualTo("20000.00")
    }
}

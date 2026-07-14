package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class CurrencySelectorOptionsFactoryTest {

    @Test
    fun `happy path - EUR integration with USD local option`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "usd",
            integrationAmount = 5099L,
            integrationCurrency = "eur",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 6106L,
                    conversionMarkupBps = 150,
                    currency = "usd",
                    presentmentExchangeRate = "1.19749",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result).isEqualTo(
            CurrencySelectorOptions(
                first = CurrencyOption(
                    code = "USD",
                    formattedAmount = "\$61.06",
                    flag = FlagContent.Emoji("🇺🇸"),
                ),
                second = CurrencyOption(
                    code = "EUR",
                    formattedAmount = "€50.99",
                    flag = FlagContent.Emoji("🇪🇺"),
                ),
                selectedCode = "USD",
                exchangeRateText = "1 EUR = 1.19749 USD",
            )
        )
    }

    @Test
    fun `returns null when localCurrencyOptions is empty`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "eur",
            integrationAmount = 5099L,
            integrationCurrency = "eur",
            localCurrencyOptions = emptyList(),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `activePresentmentCurrency matches integration currency - integration is selected and no exchange rate`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "eur",
            integrationAmount = 5099L,
            integrationCurrency = "eur",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 6106L,
                    conversionMarkupBps = 150,
                    currency = "usd",
                    presentmentExchangeRate = "1.19749",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result?.selectedCode).isEqualTo("EUR")
        assertThat(result?.exchangeRateText).isNull()
    }

    @Test
    fun `activePresentmentCurrency matches local currency - local is selected`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "usd",
            integrationAmount = 5099L,
            integrationCurrency = "eur",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 6106L,
                    conversionMarkupBps = 150,
                    currency = "usd",
                    presentmentExchangeRate = "1.19749",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result?.selectedCode).isEqualTo("USD")
    }

    @Test
    fun `zero-decimal currency JPY formats without decimal places`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "usd",
            integrationAmount = 5000L,
            integrationCurrency = "jpy",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 3500L,
                    conversionMarkupBps = 150,
                    currency = "usd",
                    presentmentExchangeRate = "0.00680",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result?.first?.formattedAmount).isEqualTo("\$35.00")
        assertThat(result?.second?.formattedAmount).isEqualTo("¥5,000")
        assertThat(result?.exchangeRateText).isEqualTo("1 JPY = 0.00680 USD")
    }

    @Test
    fun `exchange rate shown when active currency is local currency`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "eur",
            integrationAmount = 5099L,
            integrationCurrency = "usd",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 4575L,
                    conversionMarkupBps = 400,
                    currency = "eur",
                    presentmentExchangeRate = "0.897235",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result?.selectedCode).isEqualTo("EUR")
        assertThat(result?.exchangeRateText).isEqualTo("1 USD = 0.897235 EUR")
    }

    @Test
    fun `supranational XAF currency has no flag emoji`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "xaf",
            integrationAmount = 1000L,
            integrationCurrency = "xaf",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 1500L,
                    conversionMarkupBps = 100,
                    currency = "usd",
                    presentmentExchangeRate = "0.00167",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result?.first?.formattedAmount).isEqualTo("\$15.00")
        assertThat(result?.second?.formattedAmount).isEqualTo("FCFA1,000")
    }

    @Test
    fun `supranational XPF currency has no flag emoji`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "xpf",
            integrationAmount = 500L,
            integrationCurrency = "xpf",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 600L,
                    conversionMarkupBps = 100,
                    currency = "eur",
                    presentmentExchangeRate = "0.00838",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result?.first?.formattedAmount).isEqualTo("€6.00")
        assertThat(result?.second?.formattedAmount).isEqualTo("CFPF500")
    }

    @Test
    fun `currency codes are uppercased regardless of server casing`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "USD",
            integrationAmount = 1000L,
            integrationCurrency = "EUR",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 1100L,
                    conversionMarkupBps = 100,
                    currency = "USD",
                    presentmentExchangeRate = "1.10000",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = Locale.US,
            flagImages = null,
        )

        assertThat(result?.first?.code).isEqualTo("USD")
        assertThat(result?.second?.code).isEqualTo("EUR")
        assertThat(result?.selectedCode).isEqualTo("USD")
        assertThat(result?.exchangeRateText).isEqualTo("1 EUR = 1.10000 USD")
    }

    @Test
    fun `flag images used when both currencies have bitmaps`() {
        val usBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val euBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val flagImages = mapOf("USD" to usBitmap, "EUR" to euBitmap)

        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "usd",
            integrationAmount = 5099L,
            integrationCurrency = "eur",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 6106L,
                    conversionMarkupBps = 150,
                    currency = "usd",
                    presentmentExchangeRate = "1.19749",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US, flagImages)

        assertThat(result?.first?.flag).isEqualTo(FlagContent.Image(usBitmap))
        assertThat(result?.second?.flag).isEqualTo(FlagContent.Image(euBitmap))
        assertThat(result?.first?.formattedAmount).isEqualTo("\$61.06")
        assertThat(result?.second?.formattedAmount).isEqualTo("€50.99")
    }

    @Test
    fun `flag images not used when one currency bitmap is missing`() {
        val usBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val flagImages = mapOf("USD" to usBitmap)

        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "usd",
            integrationAmount = 5099L,
            integrationCurrency = "eur",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 6106L,
                    conversionMarkupBps = 150,
                    currency = "usd",
                    presentmentExchangeRate = "1.19749",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US, flagImages)

        assertThat(result?.first?.flag).isInstanceOf(FlagContent.Emoji::class.java)
        assertThat(result?.second?.flag).isInstanceOf(FlagContent.Emoji::class.java)
    }

    @Test
    fun `flag images not used when null`() {
        val adaptivePricingInfo = CheckoutSessionResponse.AdaptivePricingInfo(
            activePresentmentCurrency = "usd",
            integrationAmount = 5099L,
            integrationCurrency = "eur",
            localCurrencyOptions = listOf(
                CheckoutSessionResponse.LocalCurrencyOption(
                    amount = 6106L,
                    conversionMarkupBps = 150,
                    currency = "usd",
                    presentmentExchangeRate = "1.19749",
                )
            ),
        )

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US, null)

        assertThat(result?.first?.flag).isInstanceOf(FlagContent.Emoji::class.java)
        assertThat(result?.second?.flag).isInstanceOf(FlagContent.Emoji::class.java)
    }
}

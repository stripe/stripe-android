package com.stripe.android.paymentsheet.verticalmode

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import java.util.Locale
import kotlin.test.Test

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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

        assertThat(result).isEqualTo(
            CurrencySelectorOptions(
                first = CurrencyOption(code = "EUR", displayableText = "🇪🇺 €50.99"),
                second = CurrencyOption(code = "USD", displayableText = "🇺🇸 $61.06"),
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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

        assertThat(result?.first).isEqualTo(CurrencyOption(code = "JPY", displayableText = "🇯🇵 ¥5,000"))
        assertThat(result?.second).isEqualTo(CurrencyOption(code = "USD", displayableText = "🇺🇸 $35.00"))
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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

        assertThat(result?.first?.displayableText).isEqualTo("FCFA1,000")
        assertThat(result?.second?.displayableText).isEqualTo("🇺🇸 $15.00")
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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

        assertThat(result?.first?.displayableText).isEqualTo("CFPF500")
        assertThat(result?.second?.displayableText).isEqualTo("🇪🇺 €6.00")
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

        val result = CurrencySelectorOptionsFactory.create(adaptivePricingInfo, Locale.US)

        assertThat(result?.first?.code).isEqualTo("EUR")
        assertThat(result?.second?.code).isEqualTo("USD")
        assertThat(result?.selectedCode).isEqualTo("USD")
        assertThat(result?.exchangeRateText).isEqualTo("1 EUR = 1.10000 USD")
    }
}

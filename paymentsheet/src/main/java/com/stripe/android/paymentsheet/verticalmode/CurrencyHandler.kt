package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.AdaptivePricingInfo
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.format.CurrencyFormatter
import java.util.Locale

internal class CurrencyHandler(
    adaptivePricingInfo: AdaptivePricingInfo?,
    private val onCurrencyChanged: ((String) -> Unit)? = null,
) {
    val currencySelectorOptions: CurrencySelectorOptions? =
        buildCurrencySelectorOptions(adaptivePricingInfo)

    fun onCurrencySelected(currency: CurrencyOption) {
        onCurrencyChanged?.invoke(currency.code)
    }

    companion object {
        @OptIn(CheckoutSessionPreview::class)
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            onCurrencyChanged: ((String) -> Unit)? = null,
        ): CurrencyHandler {
            val checkoutSession =
                paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession
            val adaptivePricingInfo = checkoutSession?.let { session ->
                CheckoutInstances[session.instancesKey]
                    .firstOrNull()
                    ?.internalState
                    ?.checkoutSessionResponse
                    ?.adaptivePricingInfo
            }
            return CurrencyHandler(adaptivePricingInfo, onCurrencyChanged)
        }
    }
}

private fun buildCurrencySelectorOptions(info: AdaptivePricingInfo?): CurrencySelectorOptions? {
    info ?: return null
    val currencyOptions = buildCurrencyOptions(info)
    if (currencyOptions.size < 2) return null
    val selected = currencyOptions.firstOrNull { it.code == info.activePresentmentCurrency }
        ?: currencyOptions.first()
    return CurrencySelectorOptions(
        first = currencyOptions[0],
        second = currencyOptions[1],
        selectedCode = selected.code,
        exchangeRateText = buildExchangeRateText(info, selected),
    )
}

private fun buildExchangeRateText(info: AdaptivePricingInfo, selected: CurrencyOption): String? {
    if (selected.code == info.integrationCurrency) return null
    val rate = info.localCurrencyOptions
        .firstOrNull { it.currency == selected.code }
        ?.presentmentExchangeRate
        ?: return null
    return "1 ${info.integrationCurrency.uppercase()} = $rate ${selected.code.uppercase()}"
}

private fun buildCurrencyOptions(info: AdaptivePricingInfo): List<CurrencyOption> {
    return buildList {
        add(info.integrationCurrency.toCurrencyOption(info.integrationAmount))
        info.localCurrencyOptions.forEach { localOption ->
            add(localOption.currency.toCurrencyOption(localOption.amount))
        }
    }
}

private fun String.toCurrencyOption(amount: Long): CurrencyOption {
    val uppercaseCode = uppercase()
    val countryCode = currencyCodeToCountryCode(uppercaseCode)
    val flag = countryCode?.let { CountryConfig.countryCodeToEmoji(it) } ?: ""
    val formattedAmount = CurrencyFormatter.format(amount, uppercaseCode)
    return CurrencyOption(
        code = this,
        displayableText = buildString {
            if (flag.isNotEmpty()) {
                append(flag)
                append(" ")
            }
            append(formattedAmount)
        },
    )
}

private fun currencyCodeToCountryCode(currencyCode: String): String? {
    return Locale.getAvailableLocales().firstOrNull { locale ->
        runCatching { java.util.Currency.getInstance(locale) }.getOrNull()?.currencyCode == currencyCode
    }?.country?.takeIf { it.length == 2 }
}

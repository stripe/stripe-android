package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.format.CurrencyFormatter
import java.util.Locale

internal object CurrencySelectorOptionsFactory {
    fun create(
        adaptivePricingInfo: CheckoutSessionResponse.AdaptivePricingInfo,
        locale: Locale = Locale.getDefault(),
    ): CurrencySelectorOptions? {
        val localOption = adaptivePricingInfo.localCurrencyOptions.firstOrNull() ?: return null

        val integrationCode = adaptivePricingInfo.integrationCurrency.uppercase()
        val localCode = localOption.currency.uppercase()

        val integrationFlag = currencyCodeToFlagEmoji(integrationCode)
        val integrationCurrencyOption = CurrencyOption(
            code = integrationCode,
            displayableText = integrationFlag + CurrencyFormatter.format(
                amount = adaptivePricingInfo.integrationAmount,
                amountCurrencyCode = adaptivePricingInfo.integrationCurrency,
                targetLocale = locale,
            ),
        )

        val localFlag = currencyCodeToFlagEmoji(localCode)
        val localCurrencyOption = CurrencyOption(
            code = localCode,
            displayableText = localFlag + CurrencyFormatter.format(
                amount = localOption.amount,
                amountCurrencyCode = localOption.currency,
                targetLocale = locale,
            ),
        )

        val selectedCode = adaptivePricingInfo.activePresentmentCurrency.uppercase()

        val exchangeRateText = if (selectedCode != integrationCode) {
            "1 $integrationCode = ${localOption.presentmentExchangeRate} $localCode"
        } else {
            null
        }

        return CurrencySelectorOptions(
            first = integrationCurrencyOption,
            second = localCurrencyOption,
            selectedCode = selectedCode,
            exchangeRateText = exchangeRateText,
        )
    }

    private fun currencyCodeToFlagEmoji(currencyCode: String): String {
        val countryCode = when {
            currencyCode == "EUR" -> "EU"
            currencyCode.length >= 2 && !currencyCode.startsWith("X") -> currencyCode.substring(0, 2)
            else -> return ""
        }
        return CountryConfig.countryCodeToEmoji(countryCode) + " "
    }
}

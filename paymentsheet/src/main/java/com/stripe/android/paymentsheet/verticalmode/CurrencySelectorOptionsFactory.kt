package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
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

        val integrationCurrencyOption = CurrencyOption(
            code = integrationCode,
            displayableText = CurrencyFormatter.format(
                amount = adaptivePricingInfo.integrationAmount,
                amountCurrencyCode = adaptivePricingInfo.integrationCurrency,
                targetLocale = locale,
            ),
        )

        val localCurrencyOption = CurrencyOption(
            code = localCode,
            displayableText = CurrencyFormatter.format(
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
}

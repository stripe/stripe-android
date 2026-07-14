package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Bitmap
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.format.CurrencyFormatter
import java.util.Locale

internal object CurrencySelectorOptionsFactory {
    @Suppress("LongMethod")
    fun create(
        adaptivePricingInfo: CheckoutSessionResponse.AdaptivePricingInfo?,
        locale: Locale = Locale.getDefault(),
        flagImages: Map<String, Bitmap>?,
    ): CurrencySelectorOptions? {
        val localOption = adaptivePricingInfo?.localCurrencyOptions?.firstOrNull() ?: return null

        val integrationCode = adaptivePricingInfo.integrationCurrency.uppercase()
        val localCode = localOption.currency.uppercase()
        val integrationAmount = CurrencyFormatter.format(
            amount = adaptivePricingInfo.integrationAmount,
            amountCurrencyCode = adaptivePricingInfo.integrationCurrency,
            targetLocale = locale,
        )
        val localAmount = CurrencyFormatter.format(
            amount = localOption.amount,
            amountCurrencyCode = localOption.currency,
            targetLocale = locale,
        )

        val useImages = flagImages != null &&
            flagImages.containsKey(integrationCode) &&
            flagImages.containsKey(localCode)

        val integrationFlag: FlagContent = if (useImages) {
            FlagContent.Image(flagImages!!.getValue(integrationCode))
        } else {
            FlagContent.Emoji(currencyCodeToFlagEmoji(integrationCode))
        }

        val localFlag: FlagContent = if (useImages) {
            FlagContent.Image(flagImages!!.getValue(localCode))
        } else {
            FlagContent.Emoji(currencyCodeToFlagEmoji(localCode))
        }

        val integrationCurrencyOption = CurrencyOption(
            code = integrationCode,
            formattedAmount = integrationAmount,
            flag = integrationFlag,
        )

        val localCurrencyOption = CurrencyOption(
            code = localCode,
            formattedAmount = localAmount,
            flag = localFlag,
        )

        val selectedCode = adaptivePricingInfo.activePresentmentCurrency.uppercase()

        val exchangeRateText = if (selectedCode != integrationCode) {
            "1 $integrationCode = ${localOption.presentmentExchangeRate} $localCode"
        } else {
            null
        }

        return CurrencySelectorOptions(
            first = localCurrencyOption,
            second = integrationCurrencyOption,
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
        return CountryConfig.countryCodeToEmoji(countryCode)
    }
}

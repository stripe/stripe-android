package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import android.os.Build
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import java.util.Currency
import java.util.Locale

/*
 * Tells the card the currency code of the transaction taking place if the card asks for it. Provides `PaymentIntent`
 * currency code as a numeric code, otherwise just pass USD. Encodes the currency code as a binary-code decimal.
 */
internal object TransactionCurrencyCodeProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        val currencyCode = paymentMethodMetadata.amount()?.currencyCode

        return TagValueProducer.Result(
            tag = TAG_TRANSACTION_CURRENCY,
            value = BcdEncoding.encode(
                numericCurrencyCode(currencyCode).toLong(),
                NUMERIC_CODE_LENGTH,
            ),
        )
    }

    private fun numericCurrencyCode(currencyCode: String?): Int {
        if (currencyCode == null) {
            return DEFAULT_US_NUMERIC_CODE
        }

        val normalizedCode = currencyCode.uppercase(Locale.US)
        return currencyNumericCodeFromPlatform(normalizedCode)
            ?: CURRENCY_NUMERIC_CODES[normalizedCode]
            ?: DEFAULT_US_NUMERIC_CODE
    }

    private fun currencyNumericCodeFromPlatform(currencyCode: String): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return null
        }

        return runCatching {
            Currency.getInstance(currencyCode).numericCode
        }.getOrNull()
    }

    private const val TAG_TRANSACTION_CURRENCY = "5F2A"
    private const val NUMERIC_CODE_LENGTH = 2

    private const val DEFAULT_US_NUMERIC_CODE = 840

    private val CURRENCY_NUMERIC_CODES = mapOf(
        "USD" to DEFAULT_US_NUMERIC_CODE,
        "EUR" to 978,
        "GBP" to 826,
        "CAD" to 124,
        "AUD" to 36,
        "JPY" to 392,
        "CHF" to 756,
        "SEK" to 752,
        "NOK" to 578,
        "DKK" to 208,
        "NZD" to 554,
        "SGD" to 702,
        "HKD" to 344,
        "MXN" to 484,
        "BRL" to 986,
        "INR" to 356,
    )
}

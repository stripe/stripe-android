package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import java.util.Locale

/*
 *
 */
internal object TerminalCountryCodeProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        return TagValueProducer.Result(
            tag = TAG_TERMINAL_COUNTRY,
            value = BcdEncoding.encode(
                numericCountryCode().toLong(),
                NUMERIC_CODE_LENGTH,
            ),
        )
    }

    private fun numericCountryCode(): Int {
        return COUNTRY_NUMERIC_CODES[Locale.getDefault().country] ?: 0
    }

    private const val TAG_TERMINAL_COUNTRY = "9F1A"
    private const val NUMERIC_CODE_LENGTH = 2

    private val COUNTRY_NUMERIC_CODES = mapOf(
        "AT" to 40,
        "AU" to 36,
        "BE" to 56,
        "BR" to 76,
        "CA" to 124,
        "CH" to 756,
        "CZ" to 203,
        "DE" to 276,
        "DK" to 208,
        "ES" to 724,
        "FI" to 246,
        "FR" to 250,
        "GB" to 826,
        "HK" to 344,
        "IE" to 372,
        "IN" to 356,
        "IT" to 380,
        "JP" to 392,
        "MX" to 484,
        "NL" to 528,
        "NO" to 578,
        "NZ" to 554,
        "PL" to 616,
        "PT" to 620,
        "SE" to 752,
        "SG" to 702,
        "US" to 840,
    )
}

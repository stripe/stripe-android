package com.stripe.android.payments.samsungpay

import org.json.JSONObject

/**
 * Parses the raw Samsung Pay payment credential JSON into a [SamsungPayTokenRequest].
 *
 * Expected credential format:
 * ```json
 * {
 *   "3DS": {
 *     "data": "<base64 cryptogram>",
 *     "type": "S",
 *     "version": "100"
 *   },
 *   "payment_card_brand": "VI",
 *   "payment_last4_dpan": "1234",
 *   "payment_last4_fpan": "5678",
 *   "payment_currency_type": "USD"
 * }
 * ```
 */
internal object SamsungPayCredentialParser {

    fun parse(paymentCredential: String): SamsungPayTokenRequest {
        val json = JSONObject(paymentCredential)
        val threeDS = json.getJSONObject("3DS")

        return SamsungPayTokenRequest(
            rawCredential = paymentCredential,
            cryptogram = threeDS.getString("data"),
            cryptogramType = threeDS.getString("type"),
            version = threeDS.getString("version"),
            cardBrand = json.getString("payment_card_brand"),
            last4Dpan = json.getString("payment_last4_dpan"),
            last4Fpan = json.getString("payment_last4_fpan"),
            currencyType = json.getString("payment_currency_type"),
        )
    }
}

package com.stripe.android.payments.samsungpay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Contains the parsed fields from a Samsung Pay payment credential.
 *
 * Pass this to your server to create a Stripe token via `POST /v1/tokens` with:
 * - `card[tokenization_method]` = `"samsung_pay"`
 * - `card[cryptogram]` = [cryptogram]
 * - `card[number]` = the DPAN (full number, available server-side from [rawCredential])
 * - `card[exp_month]` / `card[exp_year]` = card expiry
 *
 * The [rawCredential] field contains the full Samsung Pay JSON for servers
 * that prefer to parse it themselves.
 */
@Parcelize
class SamsungPayTokenRequest(
    val rawCredential: String,
    val cryptogram: String,
    val cryptogramType: String,
    val version: String,
    val cardBrand: String,
    val last4Dpan: String,
    val last4Fpan: String,
    val currencyType: String,
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SamsungPayTokenRequest) return false
        return rawCredential == other.rawCredential &&
            cryptogram == other.cryptogram &&
            cryptogramType == other.cryptogramType &&
            version == other.version &&
            cardBrand == other.cardBrand &&
            last4Dpan == other.last4Dpan &&
            last4Fpan == other.last4Fpan &&
            currencyType == other.currencyType
    }

    override fun hashCode(): Int {
        var result = rawCredential.hashCode()
        result = 31 * result + cryptogram.hashCode()
        result = 31 * result + cryptogramType.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + cardBrand.hashCode()
        result = 31 * result + last4Dpan.hashCode()
        result = 31 * result + last4Fpan.hashCode()
        result = 31 * result + currencyType.hashCode()
        return result
    }
}

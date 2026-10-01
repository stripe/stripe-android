package com.stripe.android.crypto.onramp.model

import android.telephony.PhoneNumberUtils
import com.stripe.android.model.PaymentMethod
import java.util.Locale

internal data class WalletContactInfo(
    val email: String?,
    val phone: String?,
    val rawPhone: String?,
)

internal fun PaymentMethod.walletContactInfo(): WalletContactInfo? {
    val email = billingDetails?.email?.trim()?.takeIf { it.isNotEmpty() }
    val rawPhone = billingDetails?.phone?.takeIf { it.isNotBlank() }
    if (email == null && rawPhone == null) return null

    // Use the wallet's country, never the device locale. International numbers can be parsed
    // without a region. Unlike prefix-only conversion, this validates the number as well.
    val phone = rawPhone?.let {
        PhoneNumberUtils.formatNumberToE164(
            it,
            billingDetails?.address?.country?.trim()?.uppercase(Locale.ROOT) ?: "ZZ"
        )
    }
    return WalletContactInfo(email = email, phone = phone, rawPhone = rawPhone)
}

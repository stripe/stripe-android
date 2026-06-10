package com.stripe.android.common.nfcscan.apdu

internal data class NfcCardData(
    val cardNumber: String,
    val expirationMonth: Int,
    val expirationYear: Int,
)
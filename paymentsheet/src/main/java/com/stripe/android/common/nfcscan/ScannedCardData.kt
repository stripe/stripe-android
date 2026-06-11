package com.stripe.android.common.nfcscan

internal data class ScannedCardData(
    val cardNumber: String,
    val expirationMonth: Int,
    val expirationYear: Int,
)
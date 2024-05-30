package com.stripe.android.paymentsheet.example.playground

import androidx.compose.runtime.Stable

@Stable
data class MerchantOverrideState(
    val publicKey: String,
    val privateKey: String,
)
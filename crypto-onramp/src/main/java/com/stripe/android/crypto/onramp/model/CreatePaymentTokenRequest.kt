package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreatePaymentTokenRequest(
    @SerialName("payment_method")
    val paymentMethod: String,
)

package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.Serializable

@Serializable
internal data class CreatePaymentTokenResponse(
    val id: String
)

package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreatePaymentTokenRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,

    @SerialName("payment_method")
    val paymentMethod: String,
)

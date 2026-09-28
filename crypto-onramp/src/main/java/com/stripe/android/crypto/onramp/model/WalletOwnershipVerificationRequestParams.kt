package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WalletOwnershipVerificationRequestParams(
    @SerialName("challenge_id")
    val challengeId: String,
    val signature: String,
    val credentials: CryptoCustomerRequestParams.Credentials,
)

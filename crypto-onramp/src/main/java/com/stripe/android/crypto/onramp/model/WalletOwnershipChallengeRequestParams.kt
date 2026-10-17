package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WalletOwnershipChallengeRequestParams(
    @SerialName("wallet_address")
    val walletAddress: String,
    val network: CryptoNetwork,
    val credentials: CryptoCustomerRequestParams.Credentials,
)

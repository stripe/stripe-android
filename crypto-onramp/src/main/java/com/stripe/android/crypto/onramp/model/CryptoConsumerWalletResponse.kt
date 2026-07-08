package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CryptoConsumerWalletResponse(
    val id: String,
    val network: CryptoNetwork,
    @SerialName("wallet_address")
    val walletAddress: String,
    @SerialName("verified_ownership")
    val verifiedOwnership: Boolean = false,
) {
    fun toCryptoConsumerWallet(): CryptoConsumerWallet {
        return CryptoConsumerWallet(
            id = id,
            network = network,
            walletAddress = walletAddress,
            verifiedOwnership = verifiedOwnership,
        )
    }
}

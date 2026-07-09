package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WalletOwnershipChallengeResponse(
    @SerialName("challenge_id")
    val challengeId: String,
    @SerialName("wallet_address")
    val walletAddress: String,
    val network: CryptoNetwork,
    val message: String,
    @SerialName("expires_at")
    val expiresAt: String,
) {
    fun toWalletOwnershipChallenge(): WalletOwnershipChallenge {
        return WalletOwnershipChallenge(
            challengeId = challengeId,
            walletAddress = walletAddress,
            network = network,
            message = message,
            expiresAt = expiresAt,
        )
    }
}

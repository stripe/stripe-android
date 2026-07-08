package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsumerWalletResponse(
    val id: String,
    @SerialName("wallet_address")
    val walletAddress: String,
    val network: String,
    @SerialName("verified_ownership")
    val verifiedOwnership: Boolean? = null,
) {
    fun toConsumerWallet(): ConsumerWallet {
        return ConsumerWallet(
            id = id,
            walletAddress = walletAddress,
            network = network,
            verifiedOwnership = verifiedOwnership,
        )
    }
}

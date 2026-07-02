package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * A registered crypto consumer wallet, returned after wallet registration or
 * ownership verification.
 */
@ExperimentalCryptoOnramp
data class ConsumerWallet(
    /** The unique identifier for this wallet. */
    val id: String,
    /** The wallet's blockchain address. */
    val walletAddress: String,
    /** The crypto network for this wallet address. */
    val network: String,
    /**
     * Whether the merchant has proven ownership of this wallet address.
     * `null` if ownership verification has not been attempted.
     */
    val verifiedOwnership: Boolean?,
)

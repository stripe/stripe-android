package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * A short-lived challenge issued by Stripe for wallet ownership verification.
 * The merchant's wallet stack should sign the [message] field and submit the
 * signature via [OnrampCoordinator.submitWalletOwnershipSignature].
 */
@ExperimentalCryptoOnramp
data class WalletOwnershipChallenge(
    /** The unique identifier for this challenge. */
    val challengeId: String,
    /** The wallet address this challenge was issued for. */
    val walletAddress: String,
    /** The crypto network for the wallet address. */
    val network: String,
    /** The opaque message to be signed by the merchant's wallet stack. */
    val message: String,
    /** The ISO 8601 timestamp at which this challenge expires. */
    val expiresAt: String,
)

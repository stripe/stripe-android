package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko

/**
 * A short-lived Stripe-issued challenge that must be signed by the destination wallet.
 *
 * The [message] must be treated as opaque and signed exactly as returned.
 *
 * @property challengeId The identifier to use when submitting the signature.
 * @property walletAddress The wallet address bound to this challenge.
 * @property network The crypto network bound to this challenge.
 * @property message The exact opaque message to sign.
 * @property expiresAt The ISO 8601 timestamp when this challenge expires.
 */
@ExperimentalCryptoOnramp
@Poko
class WalletOwnershipChallenge internal constructor(
    val challengeId: String,
    val walletAddress: String,
    val network: CryptoNetwork,
    val message: String,
    val expiresAt: String,
)

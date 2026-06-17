package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.crypto.onramp.model.CryptoNetwork

/**
 * Indicates that checkout cannot continue until the destination wallet has completed ownership verification.
 */
@ExperimentalCryptoOnramp
class WalletOwnershipVerificationRequiredException internal constructor(
    val walletAddress: String?,
    val network: CryptoNetwork?,
) : IllegalStateException("Wallet ownership verification is required for this destination wallet.") {
    val code: String = CODE

    companion object {
        const val CODE: String = "wallet_ownership_verification_required"
    }
}

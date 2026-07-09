package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of retrieving a wallet ownership challenge.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampGetWalletOwnershipChallengeResult {
    /**
     * A wallet ownership challenge was retrieved.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val challenge: WalletOwnershipChallenge
    ) : OnrampGetWalletOwnershipChallengeResult

    /**
     * Retrieving a wallet ownership challenge failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampGetWalletOwnershipChallengeResult
}

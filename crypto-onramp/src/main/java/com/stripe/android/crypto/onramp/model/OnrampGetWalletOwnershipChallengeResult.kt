package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of requesting a wallet ownership challenge.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampGetWalletOwnershipChallengeResult {
    /**
     * Challenge was created successfully.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val challenge: WalletOwnershipChallenge
    ) : OnrampGetWalletOwnershipChallengeResult

    /**
     * Challenge creation failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampGetWalletOwnershipChallengeResult
}

package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of submitting a wallet ownership signature.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampSubmitWalletOwnershipSignatureResult {
    /**
     * Signature was submitted and ownership was verified successfully.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val wallet: ConsumerWallet
    ) : OnrampSubmitWalletOwnershipSignatureResult

    /**
     * Signature submission failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampSubmitWalletOwnershipSignatureResult
}

package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of submitting a wallet ownership signature.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampSubmitWalletOwnershipSignatureResult {
    /**
     * The signature was accepted and the updated wallet was returned.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val consumerWallet: CryptoConsumerWallet
    ) : OnrampSubmitWalletOwnershipSignatureResult

    /**
     * Submitting the signature failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampSubmitWalletOwnershipSignatureResult
}

package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of deleting a wallet address in Onramp.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampDeleteWalletAddressResult {
    /**
     * The wallet address was deleted successfully.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampDeleteWalletAddressResult

    /**
     * Deleting the wallet address failed.
     *
     * @param error The error that caused the failure.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampDeleteWalletAddressResult
}

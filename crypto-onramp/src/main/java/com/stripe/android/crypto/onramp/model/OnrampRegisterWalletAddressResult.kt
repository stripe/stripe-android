package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of registering a wallet address in Onramp.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampRegisterWalletAddressResult {
    /**
     * Wallet address was registered successfully.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampRegisterWalletAddressResult

    /**
     * Registering wallet address failed (invalid format, etc).
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampRegisterWalletAddressResult
}

package com.stripe.android.crypto.onramp.model

/**
 * Result of setting a wallet address in Onramp.
 */
sealed interface OnrampSetWalletAddressResult {
    /**
     * Wallet address was set successfully.
     */
    class Completed internal constructor() : OnrampSetWalletAddressResult

    /**
     * Setting wallet address failed (invalid format, etc).
     * @param error The error that caused the failure
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampSetWalletAddressResult
}

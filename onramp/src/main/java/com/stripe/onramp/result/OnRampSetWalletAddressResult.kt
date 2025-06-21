package com.stripe.onramp.result

/**
 * Result of setting a wallet address in OnRamp.
 */
sealed class OnRampSetWalletAddressResult {
    /**
     * Wallet address was set successfully.
     */
    object Success : OnRampSetWalletAddressResult()

    /**
     * Setting wallet address failed.
     * @param error The error that caused the failure
     */
    data class Failed(val error: Throwable) : OnRampSetWalletAddressResult()
} 
package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of setting a wallet address in Onramp.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampSetWalletAddressResult {
    /**
     * Wallet address was set successfully.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor() : OnrampSetWalletAddressResult

    /**
     * Setting wallet address failed (invalid format, etc).
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampSetWalletAddressResult
}

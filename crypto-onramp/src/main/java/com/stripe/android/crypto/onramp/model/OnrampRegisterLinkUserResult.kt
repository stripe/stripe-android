package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of an Onramp Link register user operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampRegisterLinkUserResult {
    /**
     * User registration was successful.
     * @param customerId The crypto customer ID.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val customerId: String
    ) : OnrampRegisterLinkUserResult

    /**
     * User registration failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampRegisterLinkUserResult
}

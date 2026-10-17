package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of an Onramp phone number update operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampUpdatePhoneNumberResult {
    /**
     * Phone number update was successful.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampUpdatePhoneNumberResult

    /**
     * Phone number update failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampUpdatePhoneNumberResult
}

package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of an OnRamp conguration operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampConfigurationResult {
    /**
     * Configuration was completed.
     * @param success If the configuration was successful or not.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val success: Boolean
    ) : OnrampConfigurationResult

    /**
     * Configuration failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampConfigurationResult
}

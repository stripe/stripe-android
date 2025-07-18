package com.stripe.android.crypto.onramp.model

/**
 * Callback invoked after attempting to configure the Onramp flow.
 */
fun interface OnrampConfigurationCallback {
    fun onResult(result: OnrampConfigurationResult)
}

/**
 * Result of an OnRamp conguration operation.
 */
sealed interface OnrampConfigurationResult {
    /**
     * Configuration was completed.
     * @param success If the configuration was successful or not.
     */
    class Completed internal constructor(
        val success: Boolean
    ) : OnrampConfigurationResult

    /**
     * Configuration failed.
     * @param error The error that caused the failure
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampConfigurationResult
}

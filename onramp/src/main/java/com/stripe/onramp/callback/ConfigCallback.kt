package com.stripe.onramp.callback

/**
 * Callback for OnRamp configuration results.
 */
fun interface ConfigCallback {
    /**
     * Called when OnRamp configuration is completed.
     *
     * @param success Whether the configuration was successful.
     * @param error The error that occurred during configuration, if any.
     */
    fun onConfigured(
        success: Boolean,
        error: Throwable?
    )
}
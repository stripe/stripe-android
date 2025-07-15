package com.stripe.android.crypto.onramp

/**
 * Callback invoked after attempting to configure the Onramp flow.
 */
fun interface ConfigurationCallback {
    /**
     * Called when Onramp configuration is completed.
     *
     * @param success True if configuration succeeded, false otherwise.
     * @param error The Throwable describing any failure, or null if successful.
     */
    fun onConfigured(success: Boolean, error: Throwable?)
}

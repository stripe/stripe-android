package com.stripe.android.crypto.onramp.model

fun interface OnrampLinkLookupCallback {
    fun onResult(result: OnrampLinkLookupResult)
}

/**
 * Result of an OnRamp Link user lookup operation.
 */
sealed interface OnrampLinkLookupResult {
    /**
     * Link user lookup was successful.
     * @param isLinkUser Whether the email belongs to an existing Link user
     */
    class Completed internal constructor(
        val isLinkUser: Boolean
    ) : OnrampLinkLookupResult

    /**
     * Link user lookup failed.
     * @param error The error that caused the failure
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampLinkLookupResult
}

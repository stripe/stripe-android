package com.stripe.android.crypto.onramp.model

fun interface OnrampAuthenticateUserCallback {
    fun onResult(result: OnrampAuthenticateUserResult)
}

/**
 * Result of an OnRamp User Authentication operation.
 */
sealed interface OnrampAuthenticateUserResult {
    /**
     * The link authentication was successful.
     * @param success A Boolean indicating if the authentication was successful.
     *                If `false`, the operation was cancelled.
     */
    class Completed internal constructor(
        val success: Boolean
    ) : OnrampAuthenticateUserResult

    /**
     * Authentication failed.
     * @param error The error that caused the failure
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAuthenticateUserResult
}

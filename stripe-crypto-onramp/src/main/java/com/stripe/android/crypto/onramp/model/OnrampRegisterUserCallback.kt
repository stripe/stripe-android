package com.stripe.android.crypto.onramp.model

fun interface OnrampRegisterUserCallback {
    fun onResult(result: OnrampRegisterUserResult)
}

/**
 * Result of an OnRamp Link user lookup operation.
 */
sealed interface OnrampRegisterUserResult {
    /**
     * User registration was successful.
     * @param customerId The identifier of the crypto customer that was registered.
     */
    class Completed internal constructor(
        val customerId: String
    ) : OnrampRegisterUserResult

    /**
     * User registration failed.
     * @param error The error that caused the failure
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampRegisterUserResult
}

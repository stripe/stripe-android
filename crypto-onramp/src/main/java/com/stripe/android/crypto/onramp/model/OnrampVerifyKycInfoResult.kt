package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampVerifyKycCallback {
    fun onResult(result: OnrampVerifyKycInfoResult)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampVerifyKycInfoResult {
    /**
     * KYC verified successfully.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Confirmed : OnrampVerifyKycInfoResult

    /**
     * The user indicated they need to update their address.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object UpdateAddress : OnrampVerifyKycInfoResult

    /**
     * The KYC verification was cancelled.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Cancelled : OnrampVerifyKycInfoResult

    /**
     * Verification failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampVerifyKycInfoResult
}

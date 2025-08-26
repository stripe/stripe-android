package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of KYC attachment in Onramp.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampAttachKycResult {
    /**
     * KYC submission completed successfully.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Completed : OnrampAttachKycResult

    /**
     * KYC submission failed due to an error.
     * @param error The error that caused the failure.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAttachKycResult
}

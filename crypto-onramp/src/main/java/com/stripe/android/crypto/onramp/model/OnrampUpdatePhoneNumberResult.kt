package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of an Onramp phone number update operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampUpdatePhoneNumberResult {
    /**
     * Phone number update was successful.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Completed : OnrampUpdatePhoneNumberResult

    /**
     * Phone number update failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampUpdatePhoneNumberResult
}

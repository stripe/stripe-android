package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when the KYC collection flow completes.
 */
@ExperimentalCryptoOnramp
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampKycCallback {
    fun onResult(result: OnrampKycResult)
}

/**
 * Result of collecting and submitting a KYC requirement.
 */
@ExperimentalCryptoOnramp
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampKycResult {
    /**
     * The KYC information was submitted for verification.
     */
    @ExperimentalCryptoOnramp
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Submitted internal constructor() : OnrampKycResult

    /** An existing submission is awaiting Stripe or partner review. */
    @ExperimentalCryptoOnramp
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class PendingVerification internal constructor() : OnrampKycResult

    /** A fresh requirements check found nothing to collect. */
    @ExperimentalCryptoOnramp
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class NotRequired internal constructor() : OnrampKycResult

    /**
     * The KYC collection flow was cancelled.
     */
    @ExperimentalCryptoOnramp
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampKycResult

    /**
     * Retrieving, collecting, or submitting the KYC information failed.
     */
    @ExperimentalCryptoOnramp
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable,
    ) : OnrampKycResult
}

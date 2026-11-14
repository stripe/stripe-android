package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when the KYC collection flow completes.
 */
@ExperimentalCryptoOnramp
fun interface OnrampKycCallback {
    fun onResult(result: OnrampKycResult)
}

/**
 * Result of collecting and submitting a KYC requirement.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampKycResult {
    /**
     * The KYC information was submitted for verification.
     */
    @ExperimentalCryptoOnramp
    class Submitted internal constructor() : OnrampKycResult

    /** An existing submission is awaiting Stripe or partner review. */
    @ExperimentalCryptoOnramp
    class PendingVerification internal constructor() : OnrampKycResult

    /** A fresh requirements check found nothing to collect. */
    @ExperimentalCryptoOnramp
    class NotRequired internal constructor() : OnrampKycResult

    /**
     * The KYC collection flow was cancelled.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampKycResult

    /**
     * Retrieving, collecting, or submitting the KYC information failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable,
    ) : OnrampKycResult
}

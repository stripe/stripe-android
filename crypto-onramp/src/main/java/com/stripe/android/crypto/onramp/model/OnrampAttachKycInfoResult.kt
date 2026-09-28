package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of KYC attachment in Onramp.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampAttachKycInfoResult {
    /**
     * KYC submission completed successfully.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampAttachKycInfoResult

    /**
     * KYC submission failed due to an error.
     * @param error The error that caused the failure.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAttachKycInfoResult
}

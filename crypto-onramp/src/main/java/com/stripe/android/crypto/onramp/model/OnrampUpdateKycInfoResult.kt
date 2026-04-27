package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of updating KYC info.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampUpdateKycInfoResult {
    /**
     * KYC info update completed and returned a validation result.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val result: UpdateKycInfoResult
    ) : OnrampUpdateKycInfoResult

    /**
     * KYC info update failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampUpdateKycInfoResult
}

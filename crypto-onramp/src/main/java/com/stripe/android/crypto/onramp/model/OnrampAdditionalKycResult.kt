package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when the additional KYC collection flow completes.
 */
@ExperimentalCryptoOnramp
fun interface OnrampAdditionalKycCallback {
    fun onResult(result: OnrampAdditionalKycResult)
}

/**
 * Result of collecting and submitting an additional KYC requirement.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampAdditionalKycResult {
    /**
     * The additional KYC information was submitted for verification.
     */
    @ExperimentalCryptoOnramp
    class Submitted internal constructor() : OnrampAdditionalKycResult

    /**
     * The additional KYC collection flow was cancelled.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampAdditionalKycResult

    /**
     * Retrieving, collecting, or submitting the additional KYC information failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable,
    ) : OnrampAdditionalKycResult
}

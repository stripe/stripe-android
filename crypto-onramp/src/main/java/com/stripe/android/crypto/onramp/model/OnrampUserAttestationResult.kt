package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when user attestation presentation completes.
 */
@ExperimentalCryptoOnramp
fun interface OnrampUserAttestationCallback {
    fun onResult(result: OnrampUserAttestationResult)
}

/**
 * Result of presenting and recording a user attestation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampUserAttestationResult {
    /**
     * The user attestation was accepted and recorded.
     */
    @ExperimentalCryptoOnramp
    class Confirmed internal constructor() : OnrampUserAttestationResult

    /**
     * The user attestation presentation was cancelled.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampUserAttestationResult

    /**
     * Presenting or recording the user attestation failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampUserAttestationResult
}

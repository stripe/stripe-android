package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when CRS/CARF declaration presentation completes.
 */
@ExperimentalCryptoOnramp
fun interface OnrampCrsCarfDeclarationCallback {
    fun onResult(result: OnrampCrsCarfDeclarationResult)
}

/**
 * Result of presenting and recording a CRS/CARF declaration.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampCrsCarfDeclarationResult {
    /**
     * The declaration was accepted and recorded.
     */
    @ExperimentalCryptoOnramp
    class Confirmed internal constructor() : OnrampCrsCarfDeclarationResult

    /**
     * The declaration presentation was cancelled.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampCrsCarfDeclarationResult

    /**
     * Presenting or recording the declaration failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampCrsCarfDeclarationResult
}

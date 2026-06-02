package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Common rich-error contract for Crypto Onramp failures that expose SDK-owned recovery guidance.
 *
 * [userMessage] is safe to display directly to app users. Use [developerMessage] for richer
 * diagnostics.
 */
@ExperimentalCryptoOnramp
interface StripeCryptoOnrampError {
    /**
     * An end-user-facing message, when available.
     */
    val userMessage: String

    /**
     * A richer developer-facing diagnostic message.
     */
    val developerMessage: String

    /**
     * A stable error code, when available.
     */
    val code: String?

    /**
     * A documentation URL for recovery guidance, when available.
     */
    val docUrl: String?

    /**
     * The SDK version that produced this error.
     */
    val sdkVersion: String

    /**
     * The original cause that was mapped into this richer Crypto Onramp error.
     */
    val underlyingError: Throwable?
}

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
     * A stable SDK-owned error code.
     */
    val code: String

    /**
     * A documentation URL for recovery guidance, when available.
     */
    val docUrl: String?

    /**
     * SDK versions included in developer diagnostics, including Stripe Android and any
     * additional wrapper SDK versions.
     */
    val sdkVersions: List<SDKVersion>

    /**
     * The original cause that was mapped into this richer Crypto Onramp error.
     */
    val underlyingError: Throwable?
}

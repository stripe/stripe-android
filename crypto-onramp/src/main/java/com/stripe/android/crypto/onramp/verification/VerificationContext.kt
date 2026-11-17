package com.stripe.android.crypto.onramp.verification

/**
 * Identifies the authenticated customer whose verification flow is being run.
 *
 * This is not an authentication credential. Implementations must use the authenticated session and
 * reject work if it no longer belongs to [cryptoCustomerId]. A flow must not continue for another
 * customer after logout or account switching.
 *
 * This initial contract covers customer verification only. Purchase-specific requirements and the
 * context needed to resolve them require a backend contract before they can be added here.
 */
internal data class VerificationContext(
    val cryptoCustomerId: String,
)

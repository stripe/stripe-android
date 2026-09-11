package com.stripe.android.crypto.onramp.model

import com.stripe.android.link.LinkAppearance

internal sealed interface OnrampStartKycVerificationResult {
    /**
     * Starting KYC verification completed successfully.
     */
    class Completed internal constructor(
        val response: KycRetrieveResponse,
        val appearance: LinkAppearance?
    ) : OnrampStartKycVerificationResult

    /**
     * Starting KYC verification failed due to an error.
     * @param error The error that caused the failure.
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampStartKycVerificationResult
}

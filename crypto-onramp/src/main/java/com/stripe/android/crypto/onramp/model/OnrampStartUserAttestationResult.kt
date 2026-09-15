package com.stripe.android.crypto.onramp.model

import com.stripe.android.link.LinkAppearance

internal sealed interface OnrampStartUserAttestationResult {
    /**
     * Starting user attestation presentation completed successfully.
     */
    class Completed internal constructor(
        val attestation: UserAttestation,
        val appearance: LinkAppearance?,
    ) : OnrampStartUserAttestationResult

    /**
     * Starting user attestation presentation failed.
     */
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampStartUserAttestationResult
}

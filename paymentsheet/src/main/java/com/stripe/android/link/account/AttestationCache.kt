package com.stripe.android.link.account

/**
 * In-memory singleton to cache attestation success for the app's lifetime
 */
internal object AttestationCache {
    @Volatile
    private var hasPassedAttestation = false

    fun markAttestationAsPassed() {
        hasPassedAttestation = true
    }

    fun hasPassedAttestation(): Boolean = hasPassedAttestation

    fun clear() {
        hasPassedAttestation = false
    }
}

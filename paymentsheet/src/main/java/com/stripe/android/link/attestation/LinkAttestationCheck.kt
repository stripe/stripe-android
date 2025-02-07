package com.stripe.android.link.attestation

interface LinkAttestationCheck {
    suspend fun invoke(): Result

    sealed interface Result {
        data object Successful : Result
        data class AttestationFailed(val error: Throwable) : Result
        data class Error(val error: Throwable) : Result
    }
}

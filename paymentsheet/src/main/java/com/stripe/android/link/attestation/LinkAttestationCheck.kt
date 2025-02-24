package com.stripe.android.link.attestation

internal interface LinkAttestationCheck {
    suspend fun invoke(): Result

    sealed interface Result {
        data object Successful : Result
        data class AttestationFailed(val error: Throwable) : Result
        data class AccountError(val error: Throwable) : Result
        data class Error(val error: Throwable) : Result
    }
}

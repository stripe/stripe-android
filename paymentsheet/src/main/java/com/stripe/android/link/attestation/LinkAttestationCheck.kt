package com.stripe.android.link.attestation

interface LinkAttestationCheck {
    suspend fun invoke(): Result

    sealed interface Result {
        data object Successful : Result
        data class Failed(val error: Throwable): Result
    }
}
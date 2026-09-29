package com.stripe.attestation

import androidx.annotation.RestrictTo
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AttestationTokenProvider {
    suspend fun getToken(): Result<String>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAttestationTokenProvider @Inject constructor(
    private val integrityRequestManager: IntegrityRequestManager,
) : AttestationTokenProvider {

    override suspend fun getToken(): Result<String> {
        val result = integrityRequestManager.requestToken()
        val error = result.exceptionOrNull() as? AttestationError

        return if (error?.errorType == AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID) {
            integrityRequestManager.reset()
            integrityRequestManager.requestToken()
        } else {
            result
        }
    }
}

package com.stripe.attestation

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.RetryDelaySupplier
import kotlinx.coroutines.delay

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AttestationWarmer {
    suspend fun start(): Result<Unit>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAttestationWarmer(
    private val integrityRequestManager: IntegrityRequestManager,
    private val retryDelaySupplier: RetryDelaySupplier,
) : AttestationWarmer {

    override suspend fun start(): Result<Unit> {
        var result = integrityRequestManager.prepare()

        repeat(MAX_RETRIES) { retryAttempt ->
            val error = result.exceptionOrNull() as? AttestationError

            if (error?.errorType?.isRetriable != true) {
                return result
            }

            val remainingRetries = MAX_RETRIES - retryAttempt
            delay(
                retryDelaySupplier.getDelay(
                    maxRetries = MAX_RETRIES,
                    remainingRetries = remainingRetries
                )
            )

            result = integrityRequestManager.prepare()
        }

        return result
    }

    private companion object {
        private const val MAX_RETRIES = 3
    }
}

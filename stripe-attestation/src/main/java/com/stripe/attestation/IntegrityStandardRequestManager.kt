package com.stripe.attestation

import androidx.annotation.RestrictTo
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntegrityRequestManager {
    /**
     * Prepare the integrity token. This warms up the integrity token generation, it's recommended
     * to call it as soon as possible if you know you will need an integrity token.
     *
     * Needs to be called before calling [requestToken].
     */
    suspend fun prepare(): Result<Unit>

    /**
     * Requests an Integrity token.
     *
     * @param requestIdentifier A string to be hashed to generate a request identifier.
     * Can be null. Provide a value that identifies the API request
     * to protect it from tampering attacks.
     *
     *  [Docs](https://developer.android.com/google/play/integrity/standard#protect-requests)
     */
    suspend fun requestToken(requestIdentifier: String? = null): Result<String>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntegrityStandardRequestManager(
    private val integrityTokenProviderFactory: IntegrityTokenProviderFactory,
    private val integrityTokenProviderWarmer: IntegrityTokenProviderWarmer,
    private val logError: (String, Throwable) -> Unit
) : IntegrityRequestManager {

    override suspend fun prepare(): Result<Unit> {
        return integrityTokenProviderWarmer.warmup()
    }

    override suspend fun requestToken(
        requestIdentifier: String?,
    ): Result<String> = request(requestIdentifier)

    private suspend fun request(
        requestHash: String?,
    ): Result<String> = runCatching {
        val integrityTokenProvider = integrityTokenProviderFactory.integrityTokenProvider(allowRetry = false)
            .getOrThrow()
        val finishedTask = integrityTokenProvider.request(
            StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        ).awaitTask()

        finishedTask.toResult().getOrThrow()
    }.map { it.token() }
        .recoverCatching {
            logError("Integrity - Failed to request integrity token", it)
            throw (it as? AttestationError ?: AttestationError.fromException(it))
        }
}

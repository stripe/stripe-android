package com.stripe.attestation

import androidx.annotation.RestrictTo
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
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
    private val cloudProjectNumber: Long,
    private val logError: (String, Throwable) -> Unit,
    private val factory: StandardIntegrityManagerFactory
) : IntegrityRequestManager {

    private val standardIntegrityManager: StandardIntegrityManager by lazy { factory.create() }
    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null

    override suspend fun prepare(): Result<Unit> = runCatching {
        val finishedTask: Task<StandardIntegrityTokenProvider> = standardIntegrityManager
            .prepareIntegrityToken(
                PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(cloudProjectNumber)
                    .build()
            ).awaitTask()

        finishedTask.toResult()
            .onSuccess { integrityTokenProvider = it }
            .onFailure { error -> logError("Integrity: Failed to prepare integrity token", error) }
            .getOrThrow()
    }

    override suspend fun requestToken(
        requestIdentifier: String?,
    ): Result<String> = request(requestIdentifier)

    private suspend fun request(
        requestHash: String?,
    ): Result<String> = runCatching {
        val finishedTask = requireNotNull(
            value = integrityTokenProvider,
            lazyMessage = { "Integrity token provider is not initialized. Call prepare() first." }
        ).request(
            StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        ).awaitTask()

        finishedTask.toResult()
            .mapCatching { it.token() }
            .onFailure { error -> logError("Integrity - Failed to request integrity token", error) }
            .getOrThrow()
    }
}

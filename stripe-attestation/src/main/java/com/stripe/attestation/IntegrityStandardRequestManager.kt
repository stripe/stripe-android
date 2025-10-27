package com.stripe.attestation

import android.util.Log
import androidx.annotation.RestrictTo
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val factory: StandardIntegrityManagerFactory,
    private val mutex: Mutex = Mutex()
) : IntegrityRequestManager {

    private val standardIntegrityManager: StandardIntegrityManager by lazy { factory.create() }
    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null

    override suspend fun prepare(): Result<Unit> = runCatching {
        mutex.withLock {
            if (integrityTokenProvider != null) {
                Log.d("Integrity", "Integrity token already prepared - instance: $standardIntegrityManager")
                return Result.success(Unit)
            }
            Log.d("Integrity", "Preparing integrity token provider - instance: $standardIntegrityManager")
            val finishedTask: Task<StandardIntegrityTokenProvider> = standardIntegrityManager
                .prepareIntegrityToken(
                    PrepareIntegrityTokenRequest.builder()
                        .setCloudProjectNumber(cloudProjectNumber)
                        .build()
                ).awaitTask()

            finishedTask.toResult()
                .onSuccess { integrityTokenProvider = it }
                .getOrThrow()
        }
    }
        .map {}
        .recoverCatching {
            logError("Integrity - Failed to prepare integrity token", it)
            throw AttestationError.fromException(it)
        }

    override suspend fun requestToken(
        requestIdentifier: String?,
    ): Result<String> = request(requestIdentifier)

    private suspend fun request(
        requestHash: String?,
    ): Result<String> = runCatching {
        if (integrityTokenProvider == null) {
            // If prepare() hasn't been called yet, perform it preemptively.
            // This ensures callers of requestToken() don't need to explicitly prepare first.
            // This is a convenience method, but it is recommended to call prepare() early as
            // it can take a few seconds to complete.
            prepare().getOrThrow()
        }
        val finishedTask = requireNotNull(
            value = integrityTokenProvider,
            lazyMessage = { "Integrity token provider is not initialized after prepare()" }
        ).request(
            StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        ).awaitTask()

        finishedTask.toResult().getOrThrow()
    }.map { it.token() }
        .recoverCatching {
            logError("Integrity - Failed to request integrity token", it)
            throw AttestationError.fromException(it)
        }
}

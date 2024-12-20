package com.stripe.attestation

import androidx.annotation.RestrictTo
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.delay

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntegrityRequestManager {
    /**
     * Prepare the integrity token. This warms up the integrity token generation, it's recommended
     * to call it as soon as possible if you know you will need an integrity token.
     *
     * @param maxRetries The number of times to retry the request (using exponential backoff).
     * Increase this value if calls to this method are non-blocking.
     * See https://developer.android.com/google/play/integrity/error-codes#retry-logic
     *
     * Needs to be called before calling [requestToken].
     */
    suspend fun prepare(
        maxRetries: Int = 1,
    ): Result<Unit>

    /**
     * Requests an Integrity token.
     *
     * @param requestIdentifier A string to be hashed to generate a request identifier.
     * Can be null. Provide a value that identifies the API request
     * to protect it from tampering attacks.
     * @param maxRetries The number of times to retry the request (using exponential backoff).
     * Increase this value if calls to this method are non-blocking.
     * See https://developer.android.com/google/play/integrity/error-codes#retry-logic
     *
     *  [Docs](https://developer.android.com/google/play/integrity/standard#protect-requests)
     */
    suspend fun requestToken(
        requestIdentifier: String? = null,
        maxRetries: Int = 1,
    ): Result<String>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntegrityStandardRequestManager(
    private val cloudProjectNumber: Long,
    private val logError: (String, Throwable) -> Unit,
    private val factory: StandardIntegrityManagerFactory
) : IntegrityRequestManager {

    private val standardIntegrityManager: StandardIntegrityManager by lazy { factory.create() }
    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null

    override suspend fun prepare(
        retries: Int,
    ): Result<Unit> = exponentialBackoff(maxRetries = retries) {
        runCatching {
            val finishedTask = standardIntegrityManager
                .prepareIntegrityToken(
                    PrepareIntegrityTokenRequest.builder()
                        .setCloudProjectNumber(cloudProjectNumber)
                        .build()
                ).awaitTask()

            finishedTask.toResult()
                .onSuccess { integrityTokenProvider = it }
                .getOrThrow()
        }
            .map {}
            .recoverCatching {
                logError("Integrity - Failed to prepare integrity token", it)
                throw AttestationError.fromException(it)
            }
    }

    override suspend fun requestToken(
        requestIdentifier: String?,
        maxRetries: Int,
    ): Result<String> = request(requestIdentifier, maxRetries)

    private suspend fun request(
        requestHash: String?,
        maxRetries: Int,
    ): Result<String> = exponentialBackoff(
        maxRetries = maxRetries,
    ) {
        runCatching {
            val finishedTask = requireNotNull(
                value = integrityTokenProvider,
                lazyMessage = { "Integrity token provider is not initialized. Call prepare() first." }
            ).request(
                StandardIntegrityTokenRequest.builder()
                    .setRequestHash(requestHash)
                    .build()
            ).awaitTask()

            finishedTask.toResult().getOrThrow()
        }
            .map { it.token() }
            .recoverCatching {
                logError("Integrity - Failed to prepare integrity token", it)
                throw AttestationError.fromException(it)
            }
    }

    suspend fun <T> exponentialBackoff(
        maxRetries: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_DELAY,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            val result = block()

            if (result.isSuccess) {
                return result
            }

            val exception = result.exceptionOrNull()

            // Retry only if the error is retriable
            if (exception is AttestationError && exception.errorType.isRetriable) {
                logError("Retrying due to retriable error on attempt $attempt", exception)
                delay(currentDelay)
                currentDelay = (currentDelay * MULTIPLIER).toLong()
            } else {
                return result
            }
        }
        return Result.failure<T>(
            AttestationError(
                errorType = AttestationError.ErrorType.MAX_RETRIES_EXCEEDED,
                message = "Failed after $maxRetries attempts, giving up.",
                cause = null
            )
        )
    }

    companion object {
        // Constants for the retry mechanism
        private const val INITIAL_DELAY = 2000L // Start with 2 seconds
        private const val MAX_RETRIES = 2
        private const val MULTIPLIER = 2.0
    }
}

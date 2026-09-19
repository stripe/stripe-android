package com.stripe.attestation

import androidx.annotation.RestrictTo
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.RetryDelaySupplier
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntegrityTokenProviderFactory {
    suspend fun integrityTokenProvider(allowRetry: Boolean): Result<StandardIntegrityTokenProvider>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIntegrityTokenProviderFactory(
    private val cloudProjectNumber: Long,
    private val factory: StandardIntegrityManagerFactory,
    private val logger: Logger,
    private val retryDelaySupplier: RetryDelaySupplier,
    private val mutex: Mutex
) : IntegrityTokenProviderFactory, IntegrityTokenProviderWarmer {

    private val standardIntegrityManager: StandardIntegrityManager by lazy { factory.create() }
    private var integrityTokenProvider: StandardIntegrityTokenProvider? = null

    override suspend fun warmup(): Result<Unit> {
        return exponentialRetry().map { Unit }
    }

    override suspend fun integrityTokenProvider(
        allowRetry: Boolean
    ): Result<StandardIntegrityTokenProvider> {
        return if (allowRetry) {
            exponentialRetry()
        } else {
            prepareIntegrityTokenProvider()
        }
    }

    private suspend fun exponentialRetry(): Result<StandardIntegrityTokenProvider> {
        var result = prepareIntegrityTokenProvider()

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
            result = prepareIntegrityTokenProvider()
        }

        return result
    }

    private suspend fun prepareIntegrityTokenProvider(): Result<StandardIntegrityTokenProvider> = runCatching {
        mutex.withLock {
            // The mutex ensures only one coroutine executes this block at a time, but multiple
            // calls can still queue up waiting for the lock. The if-check prevents redundant work
            // by ensuring that once the first call completes and sets integrityTokenProvider, all
            // subsequent calls (that were queued) will see it's already initialized and return
            // early without re-executing the expensive prepareIntegrityToken() operation.
            val integrityTokenProvider = integrityTokenProvider
            if (integrityTokenProvider != null) {
                logger.debug("Integrity token already prepared - instance: $standardIntegrityManager")
                return@withLock integrityTokenProvider
            }
            logger.debug("Preparing integrity token provider - instance: $standardIntegrityManager")
            val finishedTask: Task<StandardIntegrityTokenProvider> = standardIntegrityManager
                .prepareIntegrityToken(
                    PrepareIntegrityTokenRequest.builder()
                        .setCloudProjectNumber(cloudProjectNumber)
                        .build()
                ).awaitTask()

            finishedTask.toResult()
                .onSuccess { this.integrityTokenProvider = it }
                .getOrThrow()
        }
    }.recoverCatching {
        logger.error("Integrity - Failed to prepare integrity token", it)
        throw AttestationError.fromException(it)
    }

    private companion object {
        private const val MAX_RETRIES = 3
    }
}

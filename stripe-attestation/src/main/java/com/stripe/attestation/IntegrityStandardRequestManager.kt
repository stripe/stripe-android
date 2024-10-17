package com.stripe.attestation

import androidx.annotation.RestrictTo
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.stripe.android.core.Logger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntegrityRequestManager {
    /**
     * Prepare the integrity token.
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
    suspend fun requestToken(requestIdentifier: String?): Result<String>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntegrityStandardRequestManager(
    private val cloudProjectNumber: Long,
    private val logger: Logger, // inject a lambda instead.
    private val factory: StandardIntegrityManagerFactory
) : IntegrityRequestManager {

    private val standardIntegrityManager: StandardIntegrityManager by lazy { factory.create() }
    private lateinit var integrityTokenProvider: StandardIntegrityTokenProvider

    override suspend fun prepare(): Result<Unit> = runCatching {
        val finishedTask: Task<StandardIntegrityTokenProvider> = standardIntegrityManager
            .prepareIntegrityToken(
                PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(cloudProjectNumber)
                    .build()
            ).awaitTask()

        return finishedTask.toResult().fold(
            onSuccess = {
                logger.debug("Integrity: Prepared integrity token successfully")
                integrityTokenProvider = it
                Result.success(Unit)
            },
            onFailure = { error ->
                logger.error("Integrity: Failed to prepare integrity token", error)
                Result.failure(error)
            }
        )
    }

    override suspend fun requestToken(
        requestIdentifier: String?,
    ): Result<String> {
        logger.debug("Integrity - Requesting integrity token request $requestIdentifier")
        return request(requestIdentifier)
    }

    private suspend fun request(
        requestHash: String?,
    ): Result<String> = runCatching {
        require(::integrityTokenProvider.isInitialized) {
            "Integrity token provider is not initialized. Call prepare() first."
        }
        val finishedTask = integrityTokenProvider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        ).awaitTask()
        return finishedTask.toResult().fold(
            onSuccess = {
                val token = it.token()
                logger.debug("Integrity - Received integrity token $token")
                Result.success(token)
            },
            onFailure = {
                logger.error("Integrity - Failed to request integrity token", it)
                Result.failure(it)
            }
        )
    }
}

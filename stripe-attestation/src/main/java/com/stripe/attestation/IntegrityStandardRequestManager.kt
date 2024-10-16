package com.stripe.attestation

import android.content.Context
import androidx.annotation.RestrictTo
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.stripe.android.core.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntegrityStandardRequestManager(
    private val cloudProjectNumber: Long,
    private val logger: Logger,
    appContext: Context
) {

    private val standardIntegrityManager by lazy { IntegrityManagerFactory.createStandard(appContext) }
    private lateinit var integrityTokenProvider: StandardIntegrityTokenProvider

    /**
     * Prepare the integrity token.
     *
     * Needs to be called before calling [requestToken].
     */
    suspend fun prepare() = suspendCancellableCoroutine<Result<Unit>> { continuation ->
        runCatching {
            standardIntegrityManager.prepareIntegrityToken(
                PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(cloudProjectNumber)
                    .build()
            )
                .addOnSuccessListener {
                    logger.debug("Integrity: Prepared integrity token successfully")
                    integrityTokenProvider = it
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener {
                    logger.error("Integrity: Failed to prepare integrity token", it)
                    continuation.resume(Result.failure(it))
                }
        }.onFailure {
            logger.error("Integrity: Failed to prepare integrity token", it)
            continuation.resume(Result.failure(it))
        }
    }

    /**
     * Requests an Integrity token.
     *
     * @param requestIdentifier A string to be hashed to generate a request identifier. Can be null. Provide a value
     * that identifies the API request to protect it from tampering attacks.
     *
     * @see https://developer.android.com/google/play/integrity/standard#protect-requests
     */
    suspend fun requestToken(
        requestIdentifier: String?,
    ): Result<String> {
        logger.debug("Integrity - Requesting integrity token request $requestIdentifier")
        return request(requestIdentifier)
    }

    private suspend fun request(
        requestHash: String?,
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        runCatching {
            integrityTokenProvider.request(
                StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                    .setRequestHash(requestHash)
                    .build()
            )
                .addOnSuccessListener { response: StandardIntegrityToken ->
                    val token = response.token()
                    logger.debug("Integrity - Received integrity token $token")
                    continuation.resume(Result.success(token))
                }
                .addOnFailureListener { exception: Exception ->
                    logger.error("Integrity - Failed to request integrity token", exception)
                    continuation.resume(Result.failure(exception))
                }
        }.onFailure {
            logger.error("Integrity - Failed to request integrity token", it)
            continuation.resume(Result.failure(it))
        }
    }
}
